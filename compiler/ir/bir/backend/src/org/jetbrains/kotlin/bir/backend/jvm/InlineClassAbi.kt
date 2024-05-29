/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.*
import org.jetbrains.kotlin.bir.util.defaultType
import org.jetbrains.kotlin.bir.util.inlineClassRepresentation

fun BirType.unboxInlineClass(): BirType = InlineClassAbi.unboxType(this) ?: this

object InlineClassAbi {
    /**
     * Unwraps inline class types to their underlying representation.
     * Returns null if the type cannot be unboxed.
     */
    fun unboxType(type: BirType): BirType? {
        val klass = type.classOrNull?.owner ?: return null
        val representation = klass.inlineClassRepresentation ?: return null

        // TODO: Apply type substitutions
        var underlyingType = representation.underlyingType.unboxInlineClass()
        if (!underlyingType.isNullable() && underlyingType.classOrNull is BirTypeParameterSymbol) {
            underlyingType = underlyingType.erasedUpperBound.symbol.defaultType
        }
        if (!type.isNullable())
            return underlyingType
        if (underlyingType.isNullable() || underlyingType.isPrimitiveType())
            return null
        return underlyingType.makeNullable()
    }
}