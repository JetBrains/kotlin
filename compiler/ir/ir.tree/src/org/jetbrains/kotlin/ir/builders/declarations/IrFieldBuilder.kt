/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders.declarations

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.types.IrType

class IrFieldBuilder : IrDeclarationBuilder() {

    lateinit var type: IrType

    var isFinal: Boolean = false
    var isExternal: Boolean = false
    var isStatic: Boolean = false
    var isFakeOverride: Boolean = false
    var metadata: MetadataSource.Property? = null

    fun updateFrom(from: IrField) {
        super.updateFrom(from)

        type = from.type
        isFinal = from.isFinal
        isExternal = from.isExternal
        isStatic = from.isStatic
        isFakeOverride = from.isFakeOverride
        metadata = from.metadata
    }
}
