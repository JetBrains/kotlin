/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.copyTypeParametersFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.resolve.jvm.checkers.PolymorphicSignatureCallChecker

internal val polymorphicSignaturePhase = makeIrFilePhase(
    ::PolymorphicSignatureLowering,
    name = "PolymorphicSignature",
    description = "Replace polymorphic methods with fake ones according to types at the call site"
)

private class PolymorphicSignatureLowering(val context: JvmBackendContext) : IrElementTransformer<PolymorphicSignatureLowering.Data>,
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
        symbol.owner.hasAnnotation(PolymorphicSignatureCallChecker.polymorphicSignatureFqName)

    private fun IrCall.transform(castReturnType: IrType?): IrCall {
        val function = symbol.owner
        assert(function.valueParameters.singleOrNull()?.varargElementType != null) {
            "@PolymorphicSignature methods should only have a single vararg argument: ${dump()}"
        }

        val values = (getValueArgument(0) as IrVararg?)?.elements?.map {
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
            dispatchReceiverParameter = function.dispatchReceiverParameter
            extensionReceiverParameter = function.extensionReceiverParameter
            for ((i, value) in values.withIndex()) {
                addValueParameter("\$$i", value.type, JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION)
            }
        }
        return IrCallImpl.fromSymbolOwner(
            startOffset, endOffset, fakeFunction.returnType, fakeFunction.symbol,
            origin = origin, superQualifierSymbol = superQualifierSymbol
        ).apply {
            copyTypeArgumentsFrom(this@transform)
            dispatchReceiver = this@transform.dispatchReceiver
            extensionReceiver = this@transform.extensionReceiver
            values.forEachIndexed(::putValueArgument)
            transformChildren(this@PolymorphicSignatureLowering, Data.NO_COERCION)
        }
    }
}
