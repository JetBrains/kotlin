/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun IrElement.desymbolize(): IrElement = transform(DesymbolizeTransformer, null)

object DesymbolizeTransformer : IrElementTransformerVoid() {
    override fun visitField(declaration: IrField): IrStatement {
        declaration.overridden.replaceAll {
            if (it is IrFieldSymbol) it.owner else it
        }
        return super.visitField(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        declaration.overridden.replaceAll {
            if (it is IrSimpleFunctionSymbol) it.owner else it
        }
        return super.visitSimpleFunction(declaration)
    }

    override fun visitClassReference(expression: IrClassReference): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrClassifierSymbol)
            IrClassReferenceImpl(
                expression.startOffset, expression.endOffset,
                expression.type,
                target.owner,
                expression.classType
            )
        else expression
        return super.visitClassReference(newExpression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrFunctionSymbol)
            IrFunctionReferenceImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.descriptor, expression.typeArgumentsCount, expression.valueArgumentsCount, expression.origin
            ).apply {
                copyTypeAndValueArgumentsFrom(expression)
                copyAttributes(expression)
            }
        else expression
        return super.visitFunctionReference(newExpression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        val newExpression = IrPropertyReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            expression.target.desymbolizeLink(),
            expression.descriptor, expression.typeArgumentsCount,
            expression.field?.desymbolizeLink(),
            expression.getter?.desymbolizeLink(),
            expression.setter?.desymbolizeLink(),
            expression.origin
        ).apply {
            copyTypeArgumentsFrom(expression)
            dispatchReceiver = expression.dispatchReceiver
            extensionReceiver = expression.extensionReceiver
            copyAttributes(expression)
        }
        return super.visitPropertyReference(newExpression)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        val newExpression = IrLocalDelegatedPropertyReferenceImpl(
            expression.startOffset, expression.endOffset, expression.type,
            expression.target.desymbolizeLink(),
            expression.delegate.desymbolizeLink(),
            expression.getter.desymbolizeLink(),
            expression.setter?.desymbolizeLink(),
            expression.origin
        ).apply {
            copyAttributes(expression)
        }
        return super.visitLocalDelegatedPropertyReference(newExpression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        val target = expression.target.desymbolizeLink()
        val irSuperQualifier = expression.irSuperQualifier?.desymbolizeLink()
        val newExpression = IrGetFieldImpl(
            expression.startOffset, expression.endOffset,
            target,
            expression.type, expression.receiver, expression.origin,
            irSuperQualifier
        )
        return super.visitGetField(newExpression)
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val newExpression = IrSetFieldImpl(
            expression.startOffset, expression.endOffset,
            expression.target.desymbolizeLink(),
            expression.receiver, expression.value, expression.type, expression.origin,
            expression.irSuperQualifier?.desymbolizeLink()
        )
        return super.visitSetField(newExpression)
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrValueSymbol)
            IrGetValueImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.origin
            )
        else expression
        return super.visitGetValue(newExpression)
    }

    override fun visitSetVariable(expression: IrSetVariable): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrVariableSymbol)
            IrSetVariableImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.value, expression.origin
            )
        else expression
        return super.visitSetVariable(newExpression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrConstructorSymbol)
            IrDelegatingConstructorCallImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.descriptor, expression.typeArgumentsCount, expression.valueArgumentsCount
            ).apply {
                copyTypeAndValueArgumentsFrom(expression)
            }
        else expression
        return super.visitDelegatingConstructorCall(newExpression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrConstructorSymbol)
            IrConstructorCallImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.descriptor,
                expression.typeArgumentsCount, expression.constructorTypeArgumentsCount, expression.valueArgumentsCount,
                expression.origin
            ).apply {
                copyTypeAndValueArgumentsFrom(expression)
            }
        else expression
        return super.visitConstructorCall(newExpression)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        val target = expression.target
        val newExpression = if (target is IrConstructorSymbol)
            IrEnumConstructorCallImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.typeArgumentsCount, expression.valueArgumentsCount
            ).apply {
                copyTypeAndValueArgumentsFrom(expression)
            }
        else expression
        return super.visitEnumConstructorCall(newExpression)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val newExpression = IrCallImpl(
            expression.startOffset, expression.endOffset, expression.type,
            expression.target.desymbolizeLink(),
            expression.descriptor,
            expression.typeArgumentsCount, expression.valueArgumentsCount, expression.origin,
            expression.irSuperQualifier?.desymbolizeLink()
        ).apply {
            copyTypeAndValueArgumentsFrom(expression)
        }
        return super.visitCall(newExpression)
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        val target = expression.irReturnTarget
        val newExpression = if (target is IrReturnTargetSymbol)
            IrReturnImpl(
                expression.startOffset, expression.endOffset, expression.type,
                target.owner,
                expression.value
            )
        else expression
        return super.visitReturn(newExpression)
    }

    private inline fun <reified Ir : IrSymbolOwner, D : DeclarationDescriptor, reified S : IrBindableSymbol<D, Ir>> Ir.desymbolizeLink() =
        if (this is S) owner else this
}