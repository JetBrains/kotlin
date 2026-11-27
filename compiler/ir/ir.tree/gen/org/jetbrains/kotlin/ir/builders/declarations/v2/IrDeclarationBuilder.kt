/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrAnnotation

@IrDeclarationBuilderDsl
abstract class IrDeclarationBuilder internal constructor() {
    var startOffset: Int = UNDEFINED_OFFSET

    var endOffset: Int = UNDEFINED_OFFSET

    var annotations: List<IrAnnotation> = []

    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED

    var parent: IrDeclarationParent? = null

    /**
     * Copies the header of [from] as is: the fields that [from] has in common with the declaration built by this builder.
     * Relations to other declarations, metadata, annotations and children aren't copied.
     */
    open fun updateFrom(from: IrDeclaration) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
    }

    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }
}
