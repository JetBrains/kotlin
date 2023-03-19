/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.propertyWithLateBinding]
 */
interface IrPropertyWithLateBinding : IrDeclaration {
    override val symbol: IrPropertySymbol

    var modality: Modality

    var getter: IrSimpleFunction?

    var setter: IrSimpleFunction?

    val isBound: Boolean

    fun acquireSymbol(symbol: IrPropertySymbol): IrProperty
}
