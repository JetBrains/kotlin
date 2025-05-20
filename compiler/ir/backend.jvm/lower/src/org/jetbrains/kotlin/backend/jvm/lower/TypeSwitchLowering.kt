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
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes

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
        if (typeSwitchDataOrNull == null) return super.visitWhen(expression)

//        return super.visitWhen(expression)

        val newExpression = transformToTypeSwitch(expression, typeSwitchDataOrNull)
        return visitExpression(newExpression)
        // TODO alternatively (may be used for unchanged expr as well):
//        newExpression.transformChildrenVoid(this)
//        return newExpression
    }

    internal class TypeSwitchData(
        val argument: IrGetValue,
        val orderedCheckedTypes: List<IrType>,
        val branchToTypeIndices: Map<IrBranch, List<Int>>
    )

    // If a given 'when' can be transformed to a typeSwitch + integer switch, returns the data
    // required for the transformation. Returns null otherwise.
    private fun getTypeSwitchDataOrNull(whenExpression: IrWhen, irBuiltins: IrBuiltIns) : TypeSwitchData? {
        var whenArgument: IrGetValue? = null

        val nonElseBranches = whenExpression.branches.filter { it !is IrElseBranch }
        val orderedCheckedTypes = arrayListOf<IrType>()
        val branchToTypeIndices = mutableMapOf<IrBranch, MutableList<Int>>()
        var nextTypeIndex = 0
        for (branch in nonElseBranches) {
            val conditions = IrWhenUtils.matchConditions<IrTypeOperatorCall>(irBuiltins.ororSymbol, branch.condition)
            { it.operator == IrTypeOperator.INSTANCEOF  && it.argument is IrGetValue }
                ?: return null
            for (condition in conditions) {
                val conditionArgument = condition.argument as IrGetValue
                if (whenArgument == null) {
                    whenArgument = conditionArgument
                } else if (whenArgument.symbol != conditionArgument.symbol) {
                    return null
                }
                branchToTypeIndices.getOrPut(branch) { arrayListOf() }.add(nextTypeIndex++)
                orderedCheckedTypes += condition.typeOperand
            }
        }

        if (whenArgument == null || orderedCheckedTypes.isEmpty()) return null

        return TypeSwitchData(whenArgument, orderedCheckedTypes, branchToTypeIndices)
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
            return when(typeIndices.size) {
                0 -> throw AssertionError("Type indices list cannot be empty")
                1 -> createEqualToIndexCondition(tempVar, typeIndices.single())
                else -> irCall(context.irBuiltIns.ororSymbol).apply {
                    typeIndices.forEachIndexed { argIndex, typeIndex ->
                        arguments[argIndex] = createEqualToIndexCondition(tempVar, typeIndex)
                    }
                }
            }
        }

        return context.createJvmIrBuilder(currentScope!!, expression).run {
            val bootstrapMethod = jdkTypeSwitchHandle
            val bootstrapMethodArguments = typeSwitchData.orderedCheckedTypes.map { kClassReference(it) }

            // TODO: check whether we can use just a single typeSwitch per class (or file?)
            //  or maybe they are cached?
            val dynamicTypeSwitchFunc = context.irFactory.buildFun {
                name = Name.identifier("typeSwitch")
                returnType = context.irBuiltIns.intType
                origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage
                // TODO maybe remove origin (see indyLambdaMetafactoryIntrinsic)
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
                if (it is IrElseBranch) {
                    // TODO: is it OK to keep parts of replaced IRs? Or use it.deepCopyWithoutPatchingParents()?
                    it
                } else {
                    val typeIndices = typeSwitchData.branchToTypeIndices[it]!!
                    assert(typeIndices.isNotEmpty())
                    // TODO: shall we copy attributes and metadata (such as offset) from old branch?
                    irBranch(createTypeCondition(tempVar, typeIndices), it.result)
                }
            }

            val newWhen = irWhen(expression.type, newBranches).also {
                // TODO: add new origin type?
                it.origin = expression.origin
            }

            irBlock {
                +tempVar
                +newWhen
            }
        }
    }

    // TODO taken from TypeOperatorLowering, maybe share instead
    private fun JvmIrBuilder.jvmMethodHandle(handle: Handle) =
        irCall(backendContext.symbols.jvmMethodHandle).apply {
            arguments[0] = irInt(handle.tag)
            arguments[1] = irString(handle.owner)
            arguments[2] = irString(handle.name)
            arguments[3] = irString(handle.desc)
            arguments[4] = irBoolean(handle.isInterface)
        }

    private val jdkTypeSwitchHandle = Handle(
        Opcodes.H_INVOKESTATIC,
        "java/lang/runtime/SwitchBootstraps",
        "typeSwitch",
        "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
        false
    )
}