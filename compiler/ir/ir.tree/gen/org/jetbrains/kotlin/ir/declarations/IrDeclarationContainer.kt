/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.declarationContainer]
 */
interface IrDeclarationContainer : IrDeclarationParent {
    /**
     * Accessing list of declaration may trigger lazy declaration list computation for lazy class,
     *   which requires computation of fake-overrides for this class. So it's unsafe to access it
     *   before IR for all sources is built (because fake-overrides of lazy classes may depend on
     *   declaration of source classes, e.g. for java source classes)
     */
    @UnsafeDuringIrConstructionAPI
    val declarations: MutableList<IrDeclaration>
}
