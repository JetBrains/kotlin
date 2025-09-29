/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JAVA_POLYMORPHIC_SIGNATURE_NAME
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom

/**
 * Replaces polymorphic methods (annotated with [java.lang.invoke.MethodHandle.PolymorphicSignature]) with fake ones according to types
 * at the call site. This is similar to how the Java compiler generates calls to such methods.
 *
 * For example:
 *
 *     val mh: MethodHandle = ...
 *     val result: String = mh.invokeExact(Foo()) as String
 *
 * In this case, [java.lang.invoke.MethodHandle.invokeExact] is a polymorphic method, which means that its declaration-site signature
 * must be discarded, and the real signature must be computed from the argument types at the call site and the expected return type.
 * It results in the following call in the bytecode:
 *
 *     invokevirtual java/lang/invoke/MethodHandle.invokeExact:(LFoo;)Ljava/lang/String;
 *
 * This lowering phase creates and calls a fake `IrFunction` which is the same as the original callee in everything except value parameter
 * types and return type.
 */
internal class PolymorphicSignatureLowering(val context: JvmBackendContext) : IrTransformer<PolymorphicSignatureLowering.Data>(),
    FileLoweringPass {
    override fun lower(irFile: IrFile) {
        if (context.config.languageVersionSettings.supportsFeature(LanguageFeature.PolymorphicSignature))
            irFile.transformChildren(this, Data(null))
    }

    class Data(val coerceToType: IrType?) {
        companion object {
            val NO_COERCION = Data(null)
        }
    }

    private fun IrTypeOperatorCall.isCast(): Boolean =
        operator != IrTypeOperator.INSTANCEOF && operator != IrTypeOperator.NOT_INSTANCEOF

    override fun visitElement(element: IrElement, data: Data): IrElement {
        element.transformChildren(this, Data.NO_COERCION)
        return element
    }

    // If the return type is Any?, then it is also polymorphic (e.g. MethodHandle.invokeExact
    // has polymorphic return type, while VarHandle.compareAndSet does not).
    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Data): IrExpression {
        val argument = expression.argument
        if (expression.isCast()) {
            val result = argument.transform(this, Data(expression.typeOperand))
            if (argument is IrCall && argument.isPolymorphicCall()) return result
            return expression.apply {
                this.argument = result
            }
        }
        return super.visitTypeOperator(expression, data)
    }

    override fun visitTry(aTry: IrTry, data: Data): IrExpression {
        aTry.tryResult = aTry.tryResult.transform(this, data)
        aTry.catches.transformInPlace(this, data)

        // If the try-catch-finally expression is under a type coercion, it needs to be pushed down only to the try and catch blocks,
        // NOT to the finally block, because the finally block is not an expression.
        aTry.finallyExpression = aTry.finallyExpression?.transform(this, Data.NO_COERCION)

        return aTry
    }

    override fun visitWhen(expression: IrWhen, data: Data): IrExpression {
        expression.branches.transformInPlace(this, data)
        return expression
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: Data): IrExpression {
        val statements = expression.statements
        for (i in 0 until statements.size) {
            val newData = if (i == statements.lastIndex) data else Data.NO_COERCION
            statements[i] = statements[i].transform(this, newData) as IrStatement
        }
        return expression
    }

    override fun visitCall(expression: IrCall, data: Data): IrElement =
        if (expression.isPolymorphicCall()) {
            expression.transform(data.coerceToType)
        } else super.visitCall(expression, Data.NO_COERCION)

    private fun IrCall.isPolymorphicCall(): Boolean =
        symbol.owner.hasAnnotation(JAVA_POLYMORPHIC_SIGNATURE_NAME)

    private fun IrCall.transform(castReturnType: IrType?): IrCall {
        val function = symbol.owner
        val (regularParameters, nonRegularParameters) = function.parameters.partition { it.kind == IrParameterKind.Regular }
        val regularParameter = regularParameters.singleOrNull()
        require(regularParameter?.varargElementType != null) {
            "@PolymorphicSignature methods should only have a single vararg argument: ${dump()}"
        }

        val values = (arguments[regularParameter] as IrVararg?)?.elements?.map {
            when (it) {
                is IrExpression -> it
                is IrSpreadElement -> it.expression // `*xs` acts as `xs` (for compatibility?)
                else -> throw AssertionError("unknown IrVarargElement: $it")
            }
        } ?: listOf()
        val fakeFunction = context.irFactory.buildFun {
            updateFrom(function)
            name = function.name
            origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
            returnType = if (function.returnType.isNullableAny()) castReturnType ?: function.returnType else function.returnType
        }.apply {
            parent = function.parent
            copyTypeParametersFrom(function)
            parameters = nonRegularParameters + values.mapIndexed { i, value ->
                buildValueParameter(this) {
                    name = Name.identifier("$$i")
                    type = value.type
                    origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
                    kind = IrParameterKind.Regular
                }
            }
        }
        return IrCallImpl.fromSymbolOwner(
            startOffset, endOffset, fakeFunction.returnType, fakeFunction.symbol,
            origin = origin, superQualifierSymbol = superQualifierSymbol
        ).apply {
            copyTypeArgumentsFrom(this@transform)
            arguments.assignFrom(nonRegularParameters.map { this@transform.arguments[it] } + values)
            transformChildren(this@PolymorphicSignatureLowering, Data.NO_COERCION)
        }
    }
}
