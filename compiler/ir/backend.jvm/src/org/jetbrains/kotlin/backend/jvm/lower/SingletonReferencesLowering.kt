/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val singletonReferencesPhase = makeIrFilePhase(
    ::SingletonReferencesLowering,
    name = "SingletonReferences",
    description = "Handle singleton references"
)

private class SingletonReferencesLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
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
        return if (candidate != null && appropriateThis != null && !isVisitingSuperConstructor(candidate)) {
            // Replace `SomeEnumClass.SomeEnumEntry` with `this`, if possible.
            //
            // SomeEnumEntry is a singleton, which is assigned (SETFIELD) to SomeEnumClass after the construction of the singleton is done.
            // Therefore, during the construction of SomeEnumEntry, SomeEnumClass.SomeEnumEntry isn't available yet. All references to it
            // must be replaced with `SomeEnumEntry.this`.
            IrGetValueImpl(expression.startOffset, expression.endOffset, expression.type, appropriateThis.symbol)
        } else {
            val entrySymbol = context.declarationFactory.getFieldForEnumEntry(expression.symbol.owner, expression.type)
            IrGetFieldImpl(expression.startOffset, expression.endOffset, entrySymbol.symbol, expression.type)
        }
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        // When in an instance method of the object, use the dispatch receiver. Do not use the dispatch
        // receiver in the constructor, as in the case of an `object` the only way it can be used is
        // in a super constructor call argument (=> it has JVM type "uninitialized this" and is not usable)
        // while for a `companion object` all initializers are in `<clinit>` and have no receiver at all.
        thisOfClass(expression.symbol.owner, false)?.let {
            return IrGetValueImpl(expression.startOffset, expression.endOffset, it.symbol)
        }
        val instanceField = if (allScopes.any { it.irElement == expression.symbol.owner })
            context.declarationFactory.getPrivateFieldForObjectInstance(expression.symbol.owner) // Constructor or static method.
        else
            context.declarationFactory.getFieldForObjectInstance(expression.symbol.owner) // Not in object scope at all.
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