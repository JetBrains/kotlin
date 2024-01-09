/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.expressions.BirBlockBody
import org.jetbrains.kotlin.bir.symbols.BirAnonymousInitializerSymbol

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.anonymousInitializer]
 */
abstract class BirAnonymousInitializer(elementClass: BirElementClass<*>) : BirImplElementBase(elementClass), BirElement, BirDeclaration, BirAnonymousInitializerSymbol {
    abstract var isStatic: Boolean
    abstract var body: BirBlockBody?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        body?.accept(data, visitor)
    }

    companion object : BirElementClass<BirAnonymousInitializer>(BirAnonymousInitializer::class.java, 1, true)
}
