/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

fun IrType.asString(): String = when(this) {
    // TODO: should each IrErrorType have own string representation?
    is IrErrorType -> "\$ErrorType\$"
    // TODO: should we prohibit user classes called dynamic?
    is IrDynamicType -> "dynamic"
    is IrSimpleType ->
        classifier.asString() +
                (if (hasQuestionMark) "?" else "") +
                (arguments.ifNotEmpty {
                    joinToString(separator = ",", prefix = "<", postfix = ">") { it.asString() }
                } ?: "")
    else -> error("Unexpected kind of IrType: " + javaClass.typeName)
}

private fun IrTypeArgument.asString(): String = when(this) {
    is IrStarProjection -> "*"
    is IrTypeProjection -> variance.label + (if (variance != Variance.INVARIANT) " " else "") + type.asString()
    else -> error("Unexpected kind of IrTypeArgument: " + javaClass.simpleName)
}

private fun IrClassifierSymbol.asString() = when (this) {
    is IrTypeParameterSymbol -> this.owner.name.asString()
    is IrClassSymbol -> this.owner.fqNameWhenAvailable!!.asString()
    else -> error("Unexpected kind of IrClassifierSymbol: " + javaClass.typeName)
}

/**
 * Returns inline class for given class or null of type is not inlined
 */
fun IrType.getJsInlinedClass(): IrClass? {
    if (this is IrSimpleType) {
        val erased = erase(this) ?: return null
        if (erased.isInline) {
            if (this.isMarkedNullable()) {
                var fieldType: IrType
                var fieldInlinedClass = erased
                while (true) {
                    fieldType = getInlineClassUnderlyingType(fieldInlinedClass)
                    if (fieldType.isMarkedNullable()) {
                        return null
                    }

                    fieldInlinedClass = fieldType.getJsInlinedClass() ?: break
                }
            }

            return erased
        }
    }
    return null
}

tailrec fun erase(type: IrType): IrClass? {
    val classifier = type.classifierOrFail

    return when (classifier) {
        is IrClassSymbol -> classifier.owner
        is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
        else -> error(classifier)
    }
}
