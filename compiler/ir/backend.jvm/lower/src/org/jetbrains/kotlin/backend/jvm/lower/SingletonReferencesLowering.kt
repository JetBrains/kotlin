/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class SingletonReferencesLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val constructingEnums = arrayListOf<IrDeclarationParent>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        constructingEnums.push(expression.symbol.owner.parent)
        val call = super.visitEnumConstructorCall(expression)
        constructingEnums.pop()
        return call
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val candidate = expression.symbol.owner.correspondingClass
        val appropriateThis = thisOfClass(candidate)
        return if (candidate != null && appropriateThis != null && isThisAccessible(candidate) && !isVisitingSuperConstructor(candidate)) {
            // Replace `SomeEnumClass.SomeEnumEntry` with `this`, if possible.
            //
            // SomeEnumEntry is a singleton, which is assigned (SETFIELD) to SomeEnumClass after the construction of the singleton is done.
            // Therefore, during the construction of SomeEnumEntry, SomeEnumClass.SomeEnumEntry isn't available yet. All references to it
            // must be replaced with `SomeEnumEntry.this`.
            IrGetValueImpl(expression.startOffset, expression.endOffset, expression.type, appropriateThis.symbol)
        } else {
            val entrySymbol = context.cachedDeclarations.getFieldForEnumEntry(expression.symbol.owner)
            IrGetFieldImpl(expression.startOffset, expression.endOffset, entrySymbol.symbol, expression.type)
        }
    }

    private fun isThisAccessible(irClass: IrClass): Boolean {
        for (scope in allScopes.asReversed()) {
            when (val irScopeElement = scope.irElement) {
                irClass ->
                    return true
                is IrClass ->
                    if (!irScopeElement.isInner && !irScopeElement.isAnonymousObject)
                        return false
                is IrField ->
                    if (irScopeElement.isStatic)
                        return false
                is IrFunction ->
                    if (irScopeElement.dispatchReceiverParameter == null && irScopeElement.visibility != DescriptorVisibilities.LOCAL)
                        return false
            }
        }
        return false
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        val instanceField = if (allScopes.any { it.irElement == expression.symbol.owner })
            context.cachedDeclarations.getPrivateFieldForObjectInstance(expression.symbol.owner) // Constructor or static method.
        else
            context.cachedDeclarations.getFieldForObjectInstance(expression.symbol.owner) // Not in object scope at all.
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceField.symbol, expression.type)
    }

    // `this` is generally available while the reference is within the lexical scope of the containing class.
    private fun thisOfClass(declaration: IrClass?, allowConstructorReceiver: Boolean = true): IrValueParameter? {
        if (declaration == null) return null
        for (scope in allScopes.reversed()) {
            when (val element = scope.irElement) {
                is IrFunction ->
                    element.dispatchReceiverParameter?.let { if (it.type.classOrNull == declaration.symbol) return it }
                is IrClass -> if (allowConstructorReceiver && element == declaration) return element.thisReceiver
            }
        }
        return null
    }

    // `this` isn't usable before `super.<init>`. Consider the example,
    //
    // 1:  enum class Test(val x: String, val closure1: () -> String) {
    // 2:    FOO("O", { FOO.x }) {
    // 3:      val y: String = run { FOO.x }
    // 4:    };
    // 5:  }
    //
    // The constructing sequence would look like the following, if the reference was lowered to `this`:
    //
    //   FOO.<init>(this) {
    //     val lambda1 = new lambda_in_line_1_type
    //     lambda_in_line_1_type.<init>(lambda1, this)
    //     Test.<init>(this, "O", lambda1)
    //     ...
    //     val lambda3 = new lambda_in_line_3_type
    //     lambda_in_line_3_type.<init>(lambda3, this)
    //   }
    //
    // Before and after `Test.<init>`, the type of `this` is `uninitializedThis` and `Test`, respectively. Therefore, passing `this` to
    // `lambda_containing_foo_x_type.<init>` results in a type mismatch. Passing `this` to `lambda_containing_foo_y_type.<init>` is fine.
    //
    // Assumptions:
    // 1. An enum entry's declaration parent is always the enum class.
    // 2. Enums are constructed in <clinit>, so there's no interleaving constructor calls from unrelated enums.
    private fun isVisitingSuperConstructor(irClass: IrClass) = irClass.parent == constructingEnums.lastOrNull()
}