/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

/**
 * [ExpectSymbolTransformer] replaces `expect` symbols in expressions with `actual` symbols. An `actual` symbol must be provided by
 * overriding [getActualClass], [getActualProperty], [getActualConstructor], and [getActualFunction].
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class ExpectSymbolTransformer : IrElementVisitorVoid {

    protected abstract fun getActualClass(descriptor: ClassDescriptor): IrClassSymbol?

    protected data class ActualPropertyResult(
        val propertySymbol: IrPropertySymbol,
        val getterSymbol: IrSimpleFunctionSymbol?,
        val setterSymbol: IrSimpleFunctionSymbol?,
    )

    protected abstract fun getActualProperty(descriptor: PropertyDescriptor): ActualPropertyResult?

    protected abstract fun getActualConstructor(descriptor: ClassConstructorDescriptor): IrConstructorSymbol?

    protected abstract fun getActualFunction(descriptor: FunctionDescriptor): IrSimpleFunctionSymbol?

    /**
     * [isTargetDeclaration] can be overridden to customize if an element referring to [declaration] should be transformed. This check
     * precedes [getActualClass], [getActualProperty], and so on.
     */
    protected open fun isTargetDeclaration(declaration: IrDeclaration): Boolean = declaration.isExpect

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        super.visitConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        super.visitDelegatingConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall) {
        super.visitEnumConstructorCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualConstructor(expression.symbol.descriptor) ?: return
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualFunction(expression.symbol.descriptor) ?: return
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        val (newSymbol, newGetter, newSetter) = getActualProperty(expression.symbol.descriptor) ?: return
        expression.symbol = newSymbol
        expression.getter = newGetter
        expression.setter = newSetter
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)
        if (!isTargetDeclaration(expression.symbol.owner)) return

        expression.symbol = getActualFunction(expression.symbol.descriptor) ?: return
    }

    override fun visitClassReference(expression: IrClassReference) {
        super.visitClassReference(expression)
        val oldSymbol = expression.symbol as? IrClassSymbol ?: return
        if (!isTargetDeclaration(oldSymbol.owner)) return

        expression.symbol = getActualClass(oldSymbol.descriptor) ?: return
    }
}
