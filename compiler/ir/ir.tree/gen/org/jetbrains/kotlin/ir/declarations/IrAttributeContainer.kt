/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.IrElement

/**
 * Represent IR element that can be copied, but must remember its original class. It is useful, for
 * example, to keep track of generated names for anonymous declarations.
 * @property attributeOwnerId original element before copying. Always satisfy following invariant
 * this.attributeOwnerId == this.attributeOwnerId.attributeOwnerId
 * @property attributeOwnerIdBeforeInline original element before inlining. Has sense only with IR
 * inliner. If element wasn't inlined contains null. Unlike, previous property doesn't have special
 * invariant and can contain a chain of declarations.
 * @sample org.jetbrains.kotlin.ir.generator.IrTree.attributeContainer
 */
interface IrAttributeContainer : IrElement {
    var attributeOwnerId: IrAttributeContainer

    var attributeOwnerIdBeforeInline: IrAttributeContainer?
}
