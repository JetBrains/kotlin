/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.IrWhenUtils
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.kClassReference
import org.jetbrains.kotlin.config.JvmWhenGenerationScheme
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes

private const val TYPE_SWITCH_CODE_FOR_NULL = -1

/**
 * Optimizes generation of type-checking 'when' expressions to 'invokedynamic' of Java's 'SwitchBootstrap.typeSwitch(..)' method followed
 * by a simple 'uint'-based switch. Requires target JVM 21+.
 */
@PhaseDescription(
    name = "TypeSwitchTransformation"
)
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

    internal class TypeSwitchData(
        val argument: IrGetValue,
        val orderedCheckedTypes: List<IrType>,
        val branchToTypeSwitchCodes: Map<IrBranch, List<Int>>
    )

    // If a given 'when' can be transformed to a typeSwitch + integer switch, returns the data
    // required for the transformation. Returns null otherwise.
    private fun getTypeSwitchDataOrNull(whenExpression: IrWhen, irBuiltins: IrBuiltIns): TypeSwitchData? {

        fun maybeWhenSubject(argument: IrExpression?) = argument is IrGetValue

        fun argumentsAreSubjectAndNullConst(arg1: IrExpression?, arg2: IrExpression?) =
            maybeWhenSubject(arg1) && arg2!!.isNullConst()

        fun argumentsAreSubjectAndNullConst(arguments: List<IrExpression?>) =
            argumentsAreSubjectAndNullConst(arguments[0], arguments[1]) || argumentsAreSubjectAndNullConst(arguments[1], arguments[0])

        fun isEqualsToNull(condition: IrExpression): Boolean =
            condition is IrCall && condition.symbol == irBuiltins.eqeqSymbol && argumentsAreSubjectAndNullConst(condition.arguments)

        fun isInstanceof(condition: IrExpression): Boolean =
            condition is IrTypeOperatorCall && condition.operator == IrTypeOperator.INSTANCEOF && maybeWhenSubject(condition.argument)

        fun getSubject(condition: IrExpression): IrGetValue = when {
            isInstanceof(condition) -> (condition as IrTypeOperatorCall).argument as IrGetValue
            isEqualsToNull(condition) -> {
                val arguments = (condition as IrCall).arguments
                when {
                    arguments[0] is IrGetValue -> arguments[0] as IrGetValue
                    arguments[1] is IrGetValue -> arguments[1] as IrGetValue
                    else -> throw IllegalStateException("Unexpected arguments for EqualsToNull condition: $arguments")
                }
            }
            else -> throw IllegalStateException("Unexpected condition: $condition")
        }

        fun setOrCheckSubject(whenSubject: IrGetValue?, condition: IrExpression): IrGetValue? {
            val conditionSubject = getSubject(condition)
            return if (whenSubject == null) {
                // store argument of the first branch as the candidate for the whole when's subject
                conditionSubject
            } else if (whenSubject.symbol == conditionSubject.symbol) {
                // so far all branches use the same subject, keep it
                whenSubject
            } else {
                // found non-matching subject in this branch, can't use typeSwitch
                null
            }
        }

        // TODO shall we break after the first else branch?
        //  PRO:  reduce number of cases in typeSwitch
        //  CONS: premature dead code elimination
        val nonElseBranches = whenExpression.branches.filterNot(::isElseBranch)

        var whenSubject: IrGetValue? = null
        val orderedCheckedTypes = arrayListOf<IrType>()
        val branchToTypeSwitchCodes = mutableMapOf<IrBranch, MutableList<Int>>()
        var nextTypeIndex = 0
        var hasNullChecks = false

        for (branch in nonElseBranches) {
            val conditions = IrWhenUtils.matchConditions(irBuiltins.ororSymbol, branch.condition)
            { isInstanceof(it) || isEqualsToNull(it) }
                ?: return null

            for (condition in conditions) {
                whenSubject = setOrCheckSubject(whenSubject, condition) ?: return null

                // in some cases (e.g. for WHEN_COMMA), several switch codes (corresponding to a type or null) may be matched
                // to a single original branch
                val switchCodesForBranch = branchToTypeSwitchCodes.getOrPut(branch) { arrayListOf() }
                if (isInstanceof(condition)) {
                    switchCodesForBranch.add(nextTypeIndex++)
                    orderedCheckedTypes += (condition as IrTypeOperatorCall).typeOperand
                } else {
                    assert(isEqualsToNull(condition))
                    switchCodesForBranch.add(TYPE_SWITCH_CODE_FOR_NULL)
                    hasNullChecks = true
                }
            }
        }

        val typeChecksThreshold = if (hasNullChecks) 1 else 2
        if (whenSubject == null || orderedCheckedTypes.size < typeChecksThreshold) {
            // for switches with only one type check, there is nu much sense of using typeSwitch
            return null
        }

        return TypeSwitchData(whenSubject, orderedCheckedTypes, branchToTypeSwitchCodes)
    }

    private fun transformToTypeSwitch(
        expression: IrWhen,
        typeSwitchData: TypeSwitchData,
    ): IrExpression {
        fun JvmIrBuilder.createEqualToIndexCondition(
            tempVar: IrVariable,
            typeIndex: Int,
        ): IrCall = irCall(context.irBuiltIns.eqeqSymbol).apply {
            arguments[0] = irGet(tempVar)
            arguments[1] = irInt(typeIndex)
        }

        fun JvmIrBuilder.createTypeCondition(tempVar: IrVariable, typeIndices: List<Int>): IrExpression {
            assert(typeIndices.isNotEmpty()) { "Type indices list cannot be empty" }
            return typeIndices
                .map { createEqualToIndexCondition(tempVar, it) }
                .reduce { acc, condition ->
                    irCall(context.irBuiltIns.ororSymbol).apply {
                        arguments[0] = acc
                        arguments[1] = condition
                    }
                }
        }

        return context.createJvmIrBuilder(currentScope!!, expression).run {
            val bootstrapMethod = jdkTypeSwitchHandle
            val bootstrapMethodArguments = typeSwitchData.orderedCheckedTypes.map(::kClassReference)

            val dynamicTypeSwitchFunc = context.irFactory.buildFun {
                name = Name.identifier("typeSwitch")
                returnType = context.irBuiltIns.intType
                origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage
                // TODO do we need to specify origin? E.g. there is no such thing in JvmSymbols.indyLambdaMetafactoryIntrinsic code
                addValueParameter("obj", context.irBuiltIns.anyNType, IrDeclarationOrigin.STUB_FOR_TYPE_SWITCH)
                addValueParameter(name = "restart", context.irBuiltIns.intType, IrDeclarationOrigin.STUB_FOR_TYPE_SWITCH)
            }

            val indyIntrinsicCall = irCall(backendContext.symbols.jvmIndyIntrinsic, context.irBuiltIns.intType).apply {
                arguments[0] = irCall(dynamicTypeSwitchFunc).apply {
                    arguments[0] = typeSwitchData.argument
                    arguments[1] = irInt(0)
                }
                arguments[1] = jvmMethodHandle(bootstrapMethod)
                arguments[2] = irVararg(context.irBuiltIns.anyType, bootstrapMethodArguments)
            }
            val tempVar = scope.createTemporaryVariable(indyIntrinsicCall, "typeSwitchCallResult")

            val newBranches = expression.branches.map {
                if (isElseBranch(it)) {
                    // TODO: is it OK to keep parts of replaced IRs? Or use it.deepCopyWithoutPatchingParents()?
                    it
                } else {
                    val typeIndices = typeSwitchData.branchToTypeSwitchCodes[it]!!
                    assert(typeIndices.isNotEmpty())
                    // TODO: shall we copy attributes and metadata (such as offset) from old branch? Same for newWhen below
                    irBranch(createTypeCondition(tempVar, typeIndices), it.result)
                }
            }

            val newWhen = irWhen(expression.type, newBranches).also {
                // TODO: add new origin type like JvmLoweredStatementOrigin.TRANSFORMED_WHEN?
                //      But the existing origin could be useful too...
                it.origin = expression.origin
            }

            irBlock {
                +tempVar
                +newWhen
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