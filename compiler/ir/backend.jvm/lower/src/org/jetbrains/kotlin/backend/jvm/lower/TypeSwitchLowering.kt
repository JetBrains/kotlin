/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.IrWhenUtils
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.kClassReference
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.config.JvmWhenGenerationScheme
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.util.isReifiedTypeParameter
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes

private const val TYPE_SWITCH_CODE_FOR_NULL = -1

/**
 * Optimizes generation of type-checking 'when' expressions to 'invokedynamic' of Java's 'SwitchBootstrap.typeSwitch(..)' method followed
 * by a simple 'uint'-based switch. Requires target JVM 21+.
 */
internal class TypeSwitchLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {

    override fun lower(irFile: IrFile) {
        if (context.config.whenGenerationScheme == JvmWhenGenerationScheme.INDY)
            irFile.transformChildrenVoid(this)
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        // top-down transformation is required because of potential flattening nested IrWhen
        val typeSwitchDataOrNull = getTypeSwitchDataOrNull(expression, context.irBuiltIns)

        val newExpression =
            if (typeSwitchDataOrNull == null) expression
            else transformToTypeSwitch(expression, typeSwitchDataOrNull)
        return newExpression.also { it.transformChildrenVoid(this) }
    }

    /**
     * A single type- or null-checking condition from the original 'when', with the corresponding code
     * returned by `typeSwitch` dynamic call.
     */
    private class SwitchCondition(
        val code: Int,
        val originalBranchCondition: IrExpression
    )

    /**
     * Data required for the transformation of a single original 'when'.
     */
    private class TypeSwitchData(
        val subjectVar: IrValueSymbol,
        val subjectType: IrType,
        val orderedCheckedTypes: List<IrType>,
        val branchToSwitchConditions: Map<IrBranch, List<SwitchCondition>>,
    )

    /**
     * If a given 'when' can be transformed to a typeSwitch + integer switch, returns the data required for
     * the transformation. Otherwise, returns `null`.
     */
    private fun getTypeSwitchDataOrNull(whenExpression: IrWhen, irBuiltins: IrBuiltIns): TypeSwitchData? {

        // for a given argument of type-checking or null-checking, tries to extract the corresponding
        // checked 'subject' argument (IrGetValue) that can be wrapped to extra IMPLICIT_CAST
        fun tryUnwrapSubject(argument: IrExpression?): IrGetValue? = when (argument) {
            is IrGetValue -> argument
            is IrTypeOperatorCall if argument.operator == IrTypeOperator.IMPLICIT_CAST -> tryUnwrapSubject(argument.argument)
            else -> null
        }

        fun unwrapSubject(argument: IrExpression?): IrGetValue = tryUnwrapSubject(argument)
            ?: throw IllegalStateException("Failed to unwrap subject from argument $argument")

        fun isSubjectCandidate(argument: IrExpression?): Boolean = tryUnwrapSubject(argument) != null

        fun argumentsAreSubjectAndNullConst(arg1: IrExpression?, arg2: IrExpression?) =
            isSubjectCandidate(arg1) && arg2?.isNullConst() ?: false

        fun argumentsAreSubjectAndNullConst(arguments: List<IrExpression?>) =
            arguments.size == 2 && (
                    argumentsAreSubjectAndNullConst(arguments[0], arguments[1]) ||
                            argumentsAreSubjectAndNullConst(arguments[1], arguments[0]))

        fun isEqualsToNull(condition: IrExpression): Boolean =
            condition is IrCall && condition.symbol == irBuiltins.eqeqSymbol && argumentsAreSubjectAndNullConst(condition.arguments)

        fun isInstanceof(condition: IrExpression): Boolean =
            condition is IrTypeOperatorCall && condition.operator == IrTypeOperator.INSTANCEOF && isSubjectCandidate(condition.argument)

        fun isIneligibleTypeForTypeSwitch(type: IrType) =
            type.isReifiedTypeParameter || TypeIntrinsics.isIntrinsicRequiredForInstanceOf(type.toIrBasedKotlinType())

        // extracts potential 'when' subject from the given condition
        fun getSubject(condition: IrExpression): IrGetValue = when {
            isInstanceof(condition) -> unwrapSubject((condition as IrTypeOperatorCall).argument)
            isEqualsToNull(condition) -> {
                val arguments = (condition as IrCall).arguments
                when {
                    arguments[0]?.isNullConst() ?: false -> unwrapSubject(arguments[1])
                    arguments[1]?.isNullConst() ?: false -> unwrapSubject(arguments[0])
                    else -> throw IllegalStateException("Unexpected arguments for EqualsToNull condition: $arguments")
                }
            }
            else -> throw IllegalStateException("Unexpected condition: $condition")
        }

        val nonElseBranches = whenExpression.branches.filterNot(::isElseBranch)

        var whenSubject: IrGetValue? = null
        val orderedCheckedTypes = arrayListOf<IrType>()
        val branchToSwitchConditions = mutableMapOf<IrBranch, MutableList<SwitchCondition>>()
        var nextTypeIndex = 0
        var hasNullChecks = false

        for (branch in nonElseBranches) {
            val conditions = IrWhenUtils.matchConditions(irBuiltins.ororSymbol, branch.condition)
            { isInstanceof(it) || isEqualsToNull(it) }
                ?: return null

            for (condition in conditions) {
                val conditionSubject = getSubject(condition)
                // found non-matching subject in this branch, can't use typeSwitch
                if (whenSubject != null && whenSubject.symbol != conditionSubject.symbol) return null
                whenSubject = conditionSubject

                // in some cases (e.g. for WHEN_COMMA), several switch codes (corresponding to a type or null) may be matched
                // to a single original branch
                val switchConditions = branchToSwitchConditions.getOrPut(branch) { arrayListOf() }
                if (isInstanceof(condition)) {
                    val type = (condition as IrTypeOperatorCall).typeOperand
                    if (isIneligibleTypeForTypeSwitch(type)) return null

                    switchConditions.add(SwitchCondition(nextTypeIndex++, condition))
                    orderedCheckedTypes += type
                } else {
                    assert(isEqualsToNull(condition))
                    switchConditions.add(SwitchCondition(TYPE_SWITCH_CODE_FOR_NULL, condition))
                    hasNullChecks = true
                }
            }
        }

        val typeChecksThreshold = if (hasNullChecks) 1 else 2
        if (whenSubject == null || orderedCheckedTypes.size < typeChecksThreshold) {
            // for switches with only one type check, there is not much sense of using typeSwitch
            return null
        }

        return TypeSwitchData(whenSubject.symbol, whenSubject.type, orderedCheckedTypes, branchToSwitchConditions)
    }

    /**
     * Perform the actual transformation of the given original 'when', using the given data.
     */
    private fun transformToTypeSwitch(whenExpression: IrWhen, typeSwitchData: TypeSwitchData): IrExpression =
        context.createJvmIrBuilder(currentScope!!, whenExpression).run {
            val bootstrapMethod = jdkTypeSwitchHandle
            val bootstrapMethodArguments = typeSwitchData.orderedCheckedTypes.map(::kClassReference)

            val dynamicTypeSwitchFunc = context.irFactory.buildFun {
                name = Name.identifier("typeSwitch")
                returnType = context.irBuiltIns.intType
                origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage
                addValueParameter("obj", context.irBuiltIns.anyNType, IrDeclarationOrigin.STUB_FOR_TYPE_SWITCH)
                addValueParameter(name = "restart", context.irBuiltIns.intType, IrDeclarationOrigin.STUB_FOR_TYPE_SWITCH)
            }

            val indyIntrinsicCall = at(UNDEFINED_OFFSET, UNDEFINED_OFFSET).run {
                irCall(backendContext.symbols.jvmIndyIntrinsic, context.irBuiltIns.intType).apply {
                    arguments[0] = irCall(dynamicTypeSwitchFunc).apply {
                        arguments[0] = irGet(typeSwitchData.subjectType, typeSwitchData.subjectVar)
                        arguments[1] = irInt(0)
                    }
                    arguments[1] = jvmMethodHandle(bootstrapMethod)
                    arguments[2] = irVararg(context.irBuiltIns.anyType, bootstrapMethodArguments)
                    origin = JvmLoweredStatementOrigin.WHEN_AS_TYPE_SWITCH
                }
            }
            val typeSwitchResultVar = scope.createTemporaryVariable(indyIntrinsicCall, "typeSwitchCallResult")

            for (branch in whenExpression.branches) {
                if (isElseBranch(branch)) continue
                val switchConditions = typeSwitchData.branchToSwitchConditions[branch]
                    ?: error("No switch conditions for branch:\n${branch.dump()}")
                branch.condition = createTypeCondition(typeSwitchResultVar, switchConditions)
            }
            whenExpression.origin = JvmLoweredStatementOrigin.WHEN_AS_TYPE_SWITCH

            irBlock {
                +typeSwitchResultVar
                +whenExpression
            }
        }

    fun <R> JvmIrBuilder.at(element: IrElement, action: JvmIrBuilder.() -> R): R =
        at(element.startOffset, element.endOffset).action()

    // creates 'tempVar == switchCondition.code' condition, with offsets of the corresponding original condition
    private fun JvmIrBuilder.createEqualToIndexCondition(tempVar: IrVariable, switchCondition: SwitchCondition) =
        at(switchCondition.originalBranchCondition) {
            irCall(context.irBuiltIns.eqeqSymbol).apply {
                arguments[0] = irGet(tempVar)
                arguments[1] = irInt(switchCondition.code)
            }
        }

    // creates 'tempVar == code1 || tempVar == code2 || ... ' condition, using codes and offsets from the given
    // list of SwitchConditions
    private fun JvmIrBuilder.createTypeCondition(tempVar: IrVariable, switchConditions: List<SwitchCondition>): IrExpression {
        assert(switchConditions.isNotEmpty()) { "switchConditions cannot be empty" }
        return switchConditions
            .map { createEqualToIndexCondition(tempVar, it) }
            .reduce { acc, condition ->
                irCall(context.irBuiltIns.ororSymbol).apply {
                    arguments[0] = acc
                    arguments[1] = condition
                    startOffset = minOf(acc.startOffset, condition.startOffset)
                    endOffset = maxOf(acc.endOffset, condition.endOffset)
                }
            }
    }

    private val jdkTypeSwitchHandle = Handle(
        Opcodes.H_INVOKESTATIC,
        "java/lang/runtime/SwitchBootstraps",
        "typeSwitch",
        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
        false
    )
}