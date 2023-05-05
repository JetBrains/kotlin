/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.unexpectedSymbolKind
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

fun IrType.asString(): String = when (this) {
    // TODO: should each IrErrorType have own string representation?
    is IrErrorType -> "\$ErrorType\$"
    // TODO: should we prohibit user classes called dynamic?
    is IrDynamicType -> "dynamic"
    is IrSimpleType ->
        classifier.asString() +
                when (nullability) {
                    SimpleTypeNullability.MARKED_NULLABLE -> "?"
                    SimpleTypeNullability.NOT_SPECIFIED -> ""
                    SimpleTypeNullability.DEFINITELY_NOT_NULL -> if (classifier is IrTypeParameterSymbol) " & Any" else ""
                } +
                (arguments.ifNotEmpty {
                    joinToString(separator = ",", prefix = "<", postfix = ">") { it.asString() }
                } ?: "")
    else -> error("Unexpected kind of IrType: " + javaClass.typeName)
}

private fun IrTypeArgument.asString(): String = when (this) {
    is IrStarProjection -> "*"
    is IrTypeProjection -> variance.label + (if (variance != Variance.INVARIANT) " " else "") + type.asString()
}

private fun IrClassifierSymbol.asString() = when (this) {
    is IrTypeParameterSymbol -> this.owner.name.asString()
    is IrClassSymbol -> this.owner.fqNameWhenAvailable!!.asString()
    is IrScriptSymbol -> unexpectedSymbolKind<IrClassifierSymbol>()
}

tailrec fun erase(type: IrType): IrClass? = when (val classifier = type.classifierOrFail) {
    is IrClassSymbol -> classifier.owner
    is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
    is IrScriptSymbol -> null
}

fun IrType.getClassRef(context: JsGenerationContext): JsNameRef =
    when (val klass = classifierOrFail.owner) {
        is IrClass ->
            if (klass.isEffectivelyExternal())
                context.getRefForExternalClass(klass)
            else
                context.getNameForClass(klass).makeRef()

        else -> context.getNameForStaticDeclaration(klass as IrDeclarationWithName).makeRef()
    }
