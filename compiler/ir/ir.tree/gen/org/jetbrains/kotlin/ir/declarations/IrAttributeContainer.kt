/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement

/**
 * Represents an IR element that can be copied, but must remember its original element. It is
 * useful, for example, to keep track of generated names for anonymous declarations.
 * @property attributeOwnerId original element before copying. Always satisfies the following
 *   invariant: `this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId`.
 * @property originalBeforeInline original element before inlining. Useful only with IR
 *   inliner. `null` if the element wasn't inlined. Unlike [attributeOwnerId], doesn't have the
 *   idempotence invariant and can contain a chain of declarations.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.attributeContainer]
 */
interface IrAttributeContainer : IrElement {
    var attributeOwnerId: IrAttributeContainer

    var originalBeforeInline: IrAttributeContainer?
}
