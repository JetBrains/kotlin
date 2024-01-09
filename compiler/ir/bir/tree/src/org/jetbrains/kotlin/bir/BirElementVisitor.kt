/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.kotlin.bir

typealias BirElementVisitorVoid = BirElementVisitorScopeVoid.(element: BirElement) -> Unit
typealias BirElementVisitor<D> = BirElementVisitorScope<D>.(element: BirElement, data: D) -> Unit

@JvmInline
value class BirElementVisitorScopeVoid(
    @PublishedApi internal val currentVisitor: BirElementVisitorVoid,
) {
    inline fun BirElement.walkInto() {
        accept(currentVisitor)
    }

    inline fun BirElement.walkIntoChildren() {
        acceptChildren<Nothing?>({ element, _ -> currentVisitor(element) }, null)
    }
}

@JvmInline
value class BirElementVisitorScope<D>(
    @PublishedApi internal val currentVisitor: BirElementVisitor<D>,
) {
    inline fun BirElement.walkInto(data: D) {
        accept(data, currentVisitor)
    }

    inline fun BirElement.walkIntoChildren(data: D) {
        acceptChildren(currentVisitor, data)
    }
}

inline fun <D> BirElement.accept(data: D, noinline visitor: BirElementVisitor<D>) {
    val scope = BirElementVisitorScope(visitor)
    visitor(scope, this, data)
}

inline fun BirElement.accept(noinline visitor: BirElementVisitorVoid) {
    val scope = BirElementVisitorScopeVoid(visitor)
    visitor(scope, this)
}

inline fun <D> BirElement.acceptChildren(data: D, noinline visitor: BirElementVisitor<D>) {
    val scope = BirElementVisitorScope(visitor)
    with(scope) {
        walkIntoChildren(data)
    }
}

inline fun BirElement.acceptChildren(noinline visitor: BirElementVisitorVoid) {
    val scope = BirElementVisitorScopeVoid(visitor)
    with(scope) {
        walkIntoChildren()
    }
}

typealias BirElementVisitorLite = BirElementVisitorScopeLite.(element: BirElementBase) -> Unit
/*internal fun interface BirElementVisitorLite {
    context(BirElementVisitorScopeLite)
    operator fun invoke(element: BirElementBase): Unit
}*/

@JvmInline
value class BirElementVisitorScopeLite(
    @PublishedApi internal val currentVisitor: BirElementVisitorLite,
) {
    inline fun BirElementBase.walkInto() {
        currentVisitor(this)
    }

    inline fun BirElementBase.walkIntoChildren() {
        acceptChildrenLite(currentVisitor)
    }
}

inline fun BirElement.acceptLite(noinline visitor: BirElementVisitorLite) {
    val scope = BirElementVisitorScopeLite(visitor)
    visitor(scope, this@acceptLite as BirElementBase)
}

internal inline fun BirElementBase.acceptChildrenLite(noinline visitor: BirElementVisitorLite) {
    val scope = BirElementVisitorScopeLite(visitor)
    with(scope) {
        walkIntoChildren()
    }
}