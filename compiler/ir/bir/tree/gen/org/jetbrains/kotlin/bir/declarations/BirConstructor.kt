/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.BirElementVisitorLite
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.acceptLite
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.constructor]
 */
interface BirConstructor : BirFunction, BirConstructorSymbol {
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor?

    var isPrimary: Boolean

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        typeParameters.acceptChildren(visitor, data)
        dispatchReceiverParameter?.accept(data, visitor)
        extensionReceiverParameter?.accept(data, visitor)
        valueParameters.acceptChildren(visitor, data)
        body?.accept(data, visitor)
    }

    override fun acceptChildrenLite(visitor: BirElementVisitorLite) {
        annotations.acceptChildrenLite(visitor)
        typeParameters.acceptChildrenLite(visitor)
        dispatchReceiverParameter?.acceptLite(visitor)
        extensionReceiverParameter?.acceptLite(visitor)
        valueParameters.acceptChildrenLite(visitor)
        body?.acceptLite(visitor)
    }

    companion object
}
