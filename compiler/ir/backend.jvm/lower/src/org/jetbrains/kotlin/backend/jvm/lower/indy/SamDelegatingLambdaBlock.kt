/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.indy

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.backend.jvm.ir.suspendFunctionOriginal
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.nonDispatchParameters
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.selectSAMOverriddenFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class SamDelegatingLambdaBlock(
    val rootExpression: IrExpression,
    val ref: IrRichFunctionReference,
)


internal class SamDelegatingLambdaBuilder(private val jvmContext: JvmBackendContext) {
    fun build(
        expression: IrExpression,
        superType: IrType,
        scopeSymbol: IrSymbol,
        parent: IrDeclarationParent
    ): SamDelegatingLambdaBlock {
        lateinit var ref: IrRichFunctionReference
        val block = jvmContext.createJvmIrBuilder(scopeSymbol, expression).run {
            //  for non-nullable samSuperType
            //  {
            //      val tmp = <expression>
            //      RichReference(
            //      invoke = fun `<anonymous>`(p1: T1, ..., pN: TN): R =
            //          tmp.invoke(p1, ..., pN)
            //      )
            //      ::`<anonymous>`
            //  }
            //  for nullable samSuperType
            //  {
            //      val tmp = <expression>
            //      if (tmp == null)
            //         null
            //      else
            //         RichReference(
            //           invoke = fun `<anonymous>`(p1: T1, ..., pN: TN): R =
            //             tmp.invoke(p1, ..., pN)
            //         )
            //  }

            irBlock(origin = IrStatementOrigin.LAMBDA) {
                val tmp = irTemporary(expression)
                ref = createDelegatingLambdaReference(expression, superType, tmp, parent)
                if (superType.isNullable()) {
                    +irIfNull(superType, irGet(tmp), irNull(), ref)
                } else {
                    +ref
                }
            }
        }
        return SamDelegatingLambdaBlock(block, ref)
    }

    private fun createDelegatingLambda(
        expression: IrExpression,
        superType: IrType,
        tmp: IrVariable,
        parent: IrDeclarationParent
    ): IrSimpleFunction {
        val superMethod = superType.getClass()?.getSingleAbstractMethod()
            ?: throw AssertionError("SAM type expected: ${superType.render()}")
        val effectiveValueParametersCount = superMethod.nonDispatchParameters.size
        val invocableFunctionClass =
            if (superMethod.isSuspend)
                jvmContext.irBuiltIns.suspendFunctionN(effectiveValueParametersCount)
            else
                jvmContext.irBuiltIns.functionN(effectiveValueParametersCount)
        val invokeFunction = invocableFunctionClass.functions.single { it.name == OperatorNameConventions.INVOKE }
        val typeSubstitutor = createTypeSubstitutor(superType)

        return jvmContext.irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = typeSubstitutor.substitute(superMethod.returnType)
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            isSuspend = superMethod.isSuspend
        }.also { lambda ->
            lambda.parameters = createLambdaValueParameters(superMethod, lambda, typeSubstitutor)
            lambda.body = jvmContext.createJvmIrBuilder(lambda.symbol, expression)
                .irBlockBody {
                    +irReturn(
                        irCall(invokeFunction).also { invokeCall ->
                            // We need to cast receiver to the function type because it might have an imaginary type like KFunction2 which
                            // is mapped to KFunction in the codegen by default, which has no 'invoke'. Looks like correct type arguments
                            // are not needed here, so we use "raw" type for simplicity. If that stops working, we'll need to compute the
                            // correct substitution of invocableFunctionClass by visiting tmp.type's hierarchy.
                            val rawFunctionType = invocableFunctionClass.typeWith()

                            invokeCall.arguments[0] = irImplicitCast(irGet(tmp), rawFunctionType)
                            for (parameterIndex in invokeFunction.nonDispatchParameters.indices) {
                                invokeCall.arguments[parameterIndex + 1] = irGet(lambda.nonDispatchParameters[parameterIndex])
                            }
                        }
                    )
                }
            lambda.parent = parent
        }
    }

    private fun createLambdaValueParameters(
        superMethod: IrSimpleFunction,
        lambda: IrSimpleFunction,
        typeSubstitutor: IrTypeSubstitutor
    ): List<IrValueParameter> = superMethod.nonDispatchParameters.map { superValueParameter ->
        buildValueParameter(lambda) {
            name = when (superValueParameter.kind) {
                IrParameterKind.ExtensionReceiver -> Name.identifier($$"$receiver")
                else -> superValueParameter.name
            }
            type = typeSubstitutor.substitute(superValueParameter.type)
            kind = superValueParameter.kind
        }
    }

    private fun JvmIrBuilder.createDelegatingLambdaReference(
        expression: IrExpression,
        superType: IrType,
        functionalValue: IrVariable,
        parent: IrDeclarationParent
    ): IrRichFunctionReference {
        return IrRichFunctionReferenceImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = superType,
            reflectionTargetSymbol = null,
            overriddenFunctionSymbol = superType.classOrFail.owner.selectSAMOverriddenFunction().suspendFunctionOriginal().symbol,
            invokeFunction = createDelegatingLambda(expression, superType, functionalValue, parent),
            origin = IrStatementOrigin.LAMBDA,
        )
    }

    private fun createTypeSubstitutor(irType: IrType): IrTypeSubstitutor {
        if (irType !is IrSimpleType)
            throw AssertionError("Simple type expected: ${irType.render()}")
        val irClassSymbol = irType.classOrNull
            ?: throw AssertionError("Class type expected: ${irType.render()}")
        return IrTypeSubstitutor(irClassSymbol.owner.typeParameters.map { it.symbol }, irType.arguments)
    }
}
