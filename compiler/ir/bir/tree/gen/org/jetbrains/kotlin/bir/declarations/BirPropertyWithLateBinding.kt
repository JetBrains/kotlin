/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.BirElementClass
import org.jetbrains.kotlin.bir.BirElementVisitor
import org.jetbrains.kotlin.bir.BirImplElementBase
import org.jetbrains.kotlin.bir.accept
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirPropertyWithLateBinding() : BirImplElementBase(), BirProperty {
    abstract val isBound: Boolean

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        backingField?.accept(data, visitor)
        getter?.accept(data, visitor)
        setter?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirPropertyWithLateBinding

    companion object : BirElementClass<BirPropertyWithLateBinding>(BirPropertyWithLateBinding::class.java, 75, true)
}
