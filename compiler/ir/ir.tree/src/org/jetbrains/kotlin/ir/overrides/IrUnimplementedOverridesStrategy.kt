/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.overrides

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol

interface IrUnimplementedOverridesStrategy {
    fun postProcessGeneratedFakeOverride(overridableMember: IrOverridableDeclaration<*>, parent: IrClass)

    object ProcessAsFakeOverrides : IrUnimplementedOverridesStrategy {
        override fun postProcessGeneratedFakeOverride(
            overridableMember: IrOverridableDeclaration<*>,
            parent: IrClass
        ) {
        }
    }
}
