/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.ir.util.unexpectedSymbolKind
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.addToStdlib.butIf

fun IrType.asString(context: JsIrBackendContext): String = when (this) {
    // TODO: should each IrErrorType have own string representation?
    is IrErrorType -> "\$ErrorType\$"
    // TODO: should we prohibit user classes called dynamic?
    is IrDynamicType -> "dynamic"
    is IrSimpleType ->
        classifier.asString(context) +
                when (nullability) {
                    SimpleTypeNullability.MARKED_NULLABLE -> "?"
                    SimpleTypeNullability.NOT_SPECIFIED -> ""
                    SimpleTypeNullability.DEFINITELY_NOT_NULL -> if (classifier is IrTypeParameterSymbol) " & Any" else ""
                } +
                (arguments.ifNotEmpty {
                    joinToString(separator = ",", prefix = "<", postfix = ">") { it.asString(context) }
                } ?: "")
    else -> error("Unexpected kind of IrType: " + javaClass.typeName)
}

private fun IrTypeArgument.asString(context: JsIrBackendContext): String = when (this) {
    is IrStarProjection -> "*"
    is IrTypeProjection -> variance.label + (if (variance != Variance.INVARIANT) " " else "") + type.asString(context)
}

private fun IrClassifierSymbol.asString(context: JsIrBackendContext): String {
    return when (this) {
        is IrTypeParameterSymbol -> this.owner.name.asString()
        is IrScriptSymbol -> unexpectedSymbolKind<IrClassifierSymbol>()
        is IrClassSymbol ->
            context.classToItsId[owner]
                ?: context.localClassNames[owner]
                ?: this.owner.fqNameWhenAvailable!!.asString()
    }
}

tailrec fun erase(type: IrType): IrClass? = when (val classifier = type.classifierOrFail) {
    is IrClassSymbol -> classifier.owner
    is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
    is IrScriptSymbol -> null
}

fun IrType.getClassRef(context: JsStaticContext): JsExpression {
    return when (val klass = classifierOrFail.owner) {
        is IrClass -> klass.getClassRef(context)
        else -> context.getNameForStaticDeclaration(klass as IrDeclarationWithName).makeRef()
    }
}

fun IrClass.getClassRef(context: JsStaticContext): JsExpression {
    return when {
        isEffectivelyExternal() -> context.getRefForExternalClass(this)
        else -> context.getNameForClass(this)
            .makeRef()
            .butIf(context.isPerFile) { JsInvocation(it) }
    }
}

fun IrConstructor.getConstructorRef(context: JsStaticContext): JsExpression {
    return context.getNameForConstructor(this)
        .makeRef()
        .butIf(context.isPerFile && !isEffectivelyExternal()) { JsInvocation(it) }
}