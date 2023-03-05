/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.name.Name

class IrConstructorBuilder @PublishedApi internal constructor(
    private val name: Name,
    buildingContext: IrBuildingContext
) : IrFunctionBuilder<IrConstructor>(buildingContext) {

    private var isPrimary: Boolean by SetAtMostOnce(false)

    @IrNodePropertyDsl
    fun primary(isPrimary: Boolean = true) {
        this.isPrimary = isPrimary
    }

    @PublishedApi
    override fun build(): IrConstructor {
        return buildingContext.irFactory.createConstructor(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = declarationOrigin,
            symbol = symbol(::IrConstructorSymbolImpl), // FIXME: Support public symbols
            name = name,
            visibility = declarationVisibility,
            returnType = IrUninitializedType, // FIXME!!!
            isInline = isInline,
            isExternal = isExternal,
            isPrimary = isPrimary,
            isExpect = isExpect,
        ).also {
            addFunctionPropertiesTo(it)
        }
    }
}
