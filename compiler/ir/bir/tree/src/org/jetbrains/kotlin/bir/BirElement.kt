/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir


sealed interface BirElementOrChildList {
    fun<D> acceptChildren(visitor: BirElementVisitor<D>, data: D)
}

interface BirElement : BirElementOrChildList {
    val sourceSpan: SourceSpan
    val parent: BirElementBase?
}


operator fun <E : BirElement, T> E.get(token: BirElementDynamicPropertyToken<E, T>): T? {
    return (this as BirElementBase).getDynamicProperty(token)
}

operator fun <E : BirElement, T> E.set(token: BirElementDynamicPropertyToken<E, T>, value: T?) {
    (this as BirElementBase).setDynamicProperty(token, value)
}

fun <E : BirElement, T> E.getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<E, T>, compute: () -> T): T {
    this as BirElementBase
    return getDynamicProperty(token) ?: compute().also {
        setDynamicProperty(token, it)
    }
}