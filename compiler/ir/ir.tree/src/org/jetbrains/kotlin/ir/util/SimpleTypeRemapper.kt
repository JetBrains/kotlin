/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

class SimpleTypeRemapper(
    private val symbolRemapper: SymbolRemapper
) : AbstractTypeRemapper() {

    override fun remapTypeOrNull(type: IrType): IrType? {
        if (type !is IrSimpleType) return null
        val symbol = symbolRemapper.getReferencedClassifier(type.classifier)
        val arguments = remapTypeArguments(type.arguments)
        if (symbol == type.classifier && arguments == null) return null
        return IrSimpleTypeImpl(
            symbol,
            type.nullability,
            arguments ?: type.arguments,
            type.annotations
        )
    }
}
