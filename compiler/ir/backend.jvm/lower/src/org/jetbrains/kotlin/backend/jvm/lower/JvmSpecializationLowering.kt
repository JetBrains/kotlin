/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.kClassReference
import org.jetbrains.kotlin.backend.jvm.mapping.specTypeParametersUsages
import org.jetbrains.kotlin.backend.jvm.mapping.toLightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irRawFunctionReference
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isJvmSpecialized
import org.jetbrains.kotlin.ir.util.isJvmSpecializedGeneric
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * The idea of specialization lowering is as follows: all values of specialized types are boxed, and unboxed values are only ever stored in
 * function parameters, local variables, and return values. All operations are performed on boxed values. Redundant boxing optimization will
 * later optimize away unnecessary cases.
 *
 * For example, the following function:
 *
 * ```kotlin
 * fun <@JvmSpecialize T> select(a: T, b: T, cond: Boolean): T {
 *     val result: T
 *     if (cond) {
 *         result = a
 *     } else {
 *         result = b
 *     }
 *     return result
 * }
 * ```
 *
 * gets lowered to
 *
 * ```kotlin
 * fun <@JvmSpecialize T> select(a: T, b: T, cond: Boolean): T {
 *     val result: T
 *     if (cond) {
 *         result = <jvm-unbox-marker>(<jvm-box-marker>(a))
 *     } else {
 *         result = <jvm-unbox-marker>(<jvm-box-marker>(b))
 *     }
 *     return <jvm-unbox-marker>(<jvm-box-marker>(result))
 * }
 * ```
 *
 * At the same time, calls to specialized functions are replaced with <jvm-indy>s, where each argument is wrapped in
 * <jvm-specialized-argument-marker> intrinsic.
 */
@PhasePrerequisites(VarargLowering::class, JvmLocalDeclarationsLowering::class)
class JvmSpecializationLowering(val backendContext: JvmBackendContext) :
    ClassLoweringPass,
    IrElementTransformerVoidWithContext() {

    override fun lower(irClass: IrClass) {
        irClass.transformChildrenVoid(this)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val valueDecl = expression.symbol.owner
        if (valueDecl.origin == IrDeclarationOrigin.DEFINED &&
            valueDecl.type.isJvmSpecializedGeneric
        ) {
            return irBuilder(expression).wrapExprInBoxMarker(expression)
        }
        return expression
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid(this)
        val valueDecl = expression.symbol.owner
        if (valueDecl.origin == IrDeclarationOrigin.DEFINED &&
            valueDecl.type.isJvmSpecializedGeneric
        ) {
            val irBuilder = irBuilder(expression)
            return irBuilder.irSet(valueDecl.symbol, irBuilder.wrapExprInUnboxMarker(expression.value, valueDecl.type))
        }
        return expression
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declaration.transformChildrenVoid(this)
        val valueDecl = declaration.symbol.owner
        val expression = declaration.initializer
        if (valueDecl.origin == IrDeclarationOrigin.DEFINED &&
            valueDecl.type.isJvmSpecializedGeneric &&
            expression != null
        ) {
            declaration.initializer = irBuilder(expression).wrapExprInUnboxMarker(expression, valueDecl.type)
        }
        return declaration
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid(this)
        val returnType = expression.returnTargetSymbol.owner.returnType(backendContext)
        if (returnType.isJvmSpecializedGeneric) {
            val irBuilder = irBuilder(expression)
            return irBuilder.irReturn(irBuilder.wrapExprInUnboxMarker(expression.value, returnType))
        }
        return expression
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.symbol.owner.isJvmSpecialized) {
            return transformSpecializedCall(expression)
        }
        return expression
    }

    private fun irBuilder(source: IrExpression) = backendContext.createJvmIrBuilder(currentScope!!, source)

    private fun JvmIrBuilder.wrapExprInBoxMarker(expr: IrExpression): IrExpression {
        return irCall(backendContext.symbols.jvmBoxMarkerIntrinsic, expr.type).apply {
            typeArguments[0] = expr.type
            arguments[0] = expr
        }
    }

    private fun JvmIrBuilder.wrapExprInUnboxMarker(expr: IrExpression, targetType: IrType): IrExpression {
        return irCall(backendContext.symbols.jvmUnboxMarkerIntrinsic, targetType).apply {
            typeArguments[0] = targetType
            arguments[0] = expr
        }
    }

    private fun transformSpecializedCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitExpression(expression: IrExpression): IrExpression {
                val irBuilder = irBuilder(expression)
                return irBuilder.irCall(backendContext.symbols.jvmMaybeUnboxMarkerIntrinsic, expression.type).apply {
                    typeArguments[0] = expression.type
                    arguments[0] = expression
                }
            }
        })

        fun mapTypeArgument(typeArgument: IrType?): LightIrType {
            if (typeArgument == null) error("specialized type is null in ${expression.render()}")
            return typeArgument.toLightIrType(backendContext) ?: error("could not convert to light type: ${typeArgument.render()}")
        }

        val callee = expression.symbol.owner

        val typeParametersStr = (callee.typeParameters zip expression.typeArguments)
            .filter { (param, _) -> param.isJvmSpecialized }
            .joinToString("\n") { (param, arg) -> "${param.index}=${mapTypeArgument(arg).encode()}" }

        val irBuilder = irBuilder(expression)

        val bootstrapMethodArguments = listOf(
            // genericImplClass
            irBuilder.kClassReference(callee.parentAsClass.defaultType),
            // genericImplMethodType
            irBuilder.irCall(backendContext.symbols.jvmOriginalMethodTypeIntrinsic, backendContext.irBuiltIns.anyType).apply {
                arguments[0] = irBuilder.irRawFunctionReference(backendContext.irBuiltIns.anyType, callee.symbol)
            },
            // specTypeParametersUsagesStr
            irBuilder.irString(callee.specTypeParametersUsages().encode()),
            // specializedTypeParametersStr
            irBuilder.irString(typeParametersStr),
        )

        return irBuilder.irCall(backendContext.symbols.jvmIndyIntrinsic, expression.type).apply {
            typeArguments[0] = expression.type
            arguments[0] = irBuilder.wrapCallInDynamicCall(expression)
            arguments[1] = irBuilder.bootstrapMethodHandle()
            arguments[2] = irBuilder.irVararg(backendContext.irBuiltIns.anyType, bootstrapMethodArguments)
        }
    }

    private fun JvmIrBuilder.bootstrapMethodHandle(): IrCall {
        val bootstrapDescriptor = "(" +
                "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                "Ljava/lang/String;" +
                "Ljava/lang/invoke/MethodType;" +
                "Ljava/lang/Class;" +
                "Ljava/lang/invoke/MethodType;" +
                "Ljava/lang/String;" +
                "Ljava/lang/String;" +
                ")Ljava/lang/invoke/CallSite;"
        return jvmMethodHandle(
            Handle(
                Opcodes.H_INVOKESTATIC,
                "kotlin/jvm/specialization/BootstrapMethods",
                "bootstrapSpecializedGeneric",
                bootstrapDescriptor,
                false,
            )
        )
    }

    private fun JvmIrBuilder.wrapCallInDynamicCall(call: IrCall): IrCall {
        val dynamicCallArguments = mutableListOf<IrExpression?>()
        val callee = call.symbol.owner

        val irDynamicCallTarget = backendContext.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.INVOKEDYNAMIC_CALL_TARGET
            name = call.symbol.owner.name
            returnType = callee.returnType
        }.apply {
            parent = backendContext.symbols.kotlinJvmInternalInvokeDynamicPackage

            var syntheticParameterIndex = 0

            parameters = (callee.parameters zip call.arguments).map { (parameter, argument) ->
                dynamicCallArguments.add(argument)

                buildValueParameter(this) {
                    name = Name.identifier("p${syntheticParameterIndex++}")
                    type = parameter.type
                    kind = IrParameterKind.Regular
                }
            }
        }

        return irCall(irDynamicCallTarget.symbol).apply { arguments.assignFrom(dynamicCallArguments) }
    }
}
