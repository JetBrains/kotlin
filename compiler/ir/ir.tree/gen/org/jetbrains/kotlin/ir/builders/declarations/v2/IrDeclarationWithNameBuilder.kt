/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.name.Name

abstract class IrDeclarationWithNameBuilder internal constructor() : IrDeclarationBuilder() {
    lateinit var name: Name

    internal val isNameInitialized: Boolean
        get() = ::name.isInitialized

    /**
     * Copies the header of [from] as is, without relations to other declarations, metadata, annotations and children.
     */
    fun copyFrom(from: IrDeclarationWithName) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
    }
}
