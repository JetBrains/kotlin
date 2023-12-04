/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.builtins.StandardNames.FqNames.replaceWith


sealed interface BirElementOrChildList {
    fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D)
}

interface BirElement : BirElementOrChildList {
    val sourceSpan: SourceSpan
    val parent: BirElementBase?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {}
    fun acceptChildrenLite(visitor: BirElementVisitorLite) {}

    fun replaceWith(new: BirElement?)
}


operator fun <E : BirElement, T> E.get(token: BirElementDynamicPropertyToken<E, T>): T? {
    return (this as BirElementBase).getDynamicProperty(token)
}

operator fun <E : BirElement, T> E.set(token: BirElementDynamicPropertyToken<E, T>, value: T?) {
    (this as BirImplElementBase).setDynamicProperty(token, value)
}

fun <E : BirElement, T> E.getOrPutDynamicProperty(token: BirElementDynamicPropertyToken<E, T>, compute: () -> T): T {
    return (this as BirImplElementBase).getOrPutDynamicProperty(token, compute)
}


fun BirElement.remove() {
    replaceWith(null)
}


fun <E : BirElement, R : BirElement> R.getBackReferences(key: BirElementBackReferencesKey<E, R>): List<E> {
    @Suppress("UNCHECKED_CAST")
    return (this as BirElementBase).getBackReferences(key) as List<E>
}