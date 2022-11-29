/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.invokeFun
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isMethodOfAny
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration
import org.jetbrains.kotlin.util.OperatorNameConventions

fun TODO(element: IrElement): Nothing = TODO(element::class.java.simpleName + " is not supported yet here")

fun IrFunction.hasStableJsName(context: JsIrBackendContext): Boolean {
    if (
        origin == JsLoweredDeclarationOrigin.BRIDGE_WITH_STABLE_NAME ||
        (this as? IrSimpleFunction)?.isMethodOfAny() == true // Handle names for special functions
    ) {
        return true
    }

    if (
        origin == JsLoweredDeclarationOrigin.JS_SHADOWED_EXPORT ||
        origin == JsLoweredDeclarationOrigin.BRIDGE_WITHOUT_STABLE_NAME ||
        origin == JsLoweredDeclarationOrigin.BRIDGE_PROPERTY_ACCESSOR
    ) {
        return false
    }

    val namedOrMissingGetter = when (this) {
        is IrSimpleFunction -> {
            val owner = correspondingPropertySymbol?.owner
            if (owner == null) {
                true
            } else {
                owner.getter?.getJsName() != null
            }
        }
        else -> true
    }

    return (isEffectivelyExternal() || getJsName() != null || isExported(context)) && namedOrMissingGetter
}

fun IrFunction.isEqualsInheritedFromAny(): Boolean =
    name == OperatorNameConventions.EQUALS &&
            dispatchReceiverParameter != null &&
            extensionReceiverParameter == null &&
            valueParameters.size == 1 &&
            valueParameters[0].type.isNullableAny()

fun IrDeclaration.hasStaticDispatch() = when (this) {
    is IrSimpleFunction -> dispatchReceiverParameter == null
    is IrProperty -> isTopLevelDeclaration
    is IrField -> isStatic
    else -> true
}

val IrValueDeclaration.isDispatchReceiver: Boolean
    get() {
        val parent = this.parent
        if (parent is IrClass)
            return true
        if (parent is IrFunction && parent.dispatchReceiverParameter == this)
            return true
        return false
    }

fun IrBody.prependFunctionCall(
    call: IrCall
) {
    when (this) {
        is IrExpressionBody -> {
            expression = JsIrBuilder.buildComposite(
                type = expression.type,
                statements = listOf(
                    call,
                    expression
                )
            )
        }
        is IrBlockBody -> {
            statements.add(
                0,
                call
            )
        }
    }
}

fun JsCommonBackendContext.findUnitGetInstanceFunction(): IrSimpleFunction =
    mapping.objectToGetInstanceFunction[irBuiltIns.unitClass.owner]!!

fun IrDeclaration.isImportedFromModuleOnly(): Boolean {
    return isTopLevel && isEffectivelyExternal() && (getJsModule() != null && !isJsNonModule() || (parent as? IrAnnotationContainer)?.getJsModule() != null)
}

fun invokeFunForLambda(call: IrCall) =
    call.extensionReceiver!!
        .type
        .getClass()!!
        .invokeFun!!
