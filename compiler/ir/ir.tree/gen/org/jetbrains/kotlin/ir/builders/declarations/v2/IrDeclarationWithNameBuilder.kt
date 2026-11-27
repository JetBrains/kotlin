/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.name.Name

abstract class IrDeclarationWithNameBuilder internal constructor() : IrDeclarationBuilder() {
    lateinit var name: Name

    internal val isNameInitialized: Boolean
        get() = ::name.isInitialized

    override fun updateFrom(from: IrDeclaration) {
        super.updateFrom(from)
        if (from is IrDeclarationWithName) {
            name = from.name
        }
    }
}
