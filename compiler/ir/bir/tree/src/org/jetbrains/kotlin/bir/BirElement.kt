/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.BirImplementationDetail


sealed interface BirElementOrChildList {
    fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D)
}

sealed interface BirElementFacade : BirElementOrChildList {
    val parent: BirElementBase?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {}
    fun acceptChildrenLite(visitor: BirElementVisitorLite) {}

    @BirImplementationDetail
    fun getElementClassInternal(): BirElementClass<*>

    fun getContainingDatabase(): BirDatabase?

    /**
     * Replaces the occurrence of this element inside its parent with [new] element,
     * or removes it from its parent, if [new] == null.
     *
     * Either [BirElement.parent] must not be null, or the element must one of the roots of a [BirDatabase].
     *
     * This operation only replaces the single place of a structural "attachment" of this element,
     * it does not replace its usages.
     *
     * The precise behaviour depends on where the element is currently stored:
     * - If [this] is a value of a child property - replace that property with [new]
     * - If [this] is inside a child element list:
     *    - If [new] == null and the list is nullable - `list.replace(this, null)`
     *    - Otherwise - `list.remove(this)`
     * - If [this] is a root of a [BirDatabase] - remove that root, and optionally add [new] as a database root.
     *
     * Example:
     * ```kotlin
     * val birField: BirField = // ...
     * val parent = birField.parent
     * assert(parent is BirClass)
     *
     * val index = parent.declarations.indexOf(birField)
     * assert(index != -1)
     *
     * val birProperty = BirPropertyImpl()
     * birField.replaceWith(birProperty)
     *
     * assert(birField.parent == parent)
     * assert(birField == parent.declarations[index])
     *
     * assert(birProperty.parent == null)
     * ```
     */
    fun replaceWith(new: BirElement?)
}

operator fun <E : BirElement, T> E.get(token: BirDynamicPropertyAccessToken<E, T>): T? {
    return (this as BirElementBase).getDynamicProperty(token)
}

operator fun <E : BirElement, T> E.set(token: BirDynamicPropertyAccessToken<E, T>, value: T?) {
    (this as BirElementBase).setDynamicProperty(token, value)
}

fun <E : BirElement, T> E.getOrPutDynamicProperty(token: BirDynamicPropertyAccessToken<E, T>, compute: () -> T): T {
    return (this as BirElementBase).getOrPutDynamicProperty(token, compute)
}

/**
 * Removes the occurrence of this element from its parent.
 *
 * This is a shortcut for `BirElement.replaceWith(null)`.
 * See [BirElement.replaceWith] for more details.
 */
fun BirElement.remove() {
    replaceWith(null)
}

/**
 * Gets a snapshot of all elements which reference [this] element, in a way specified by [key].
 *
 * Only elements which are a member of some [BirDatabase] are returned. However, the [BirDatabase]
 * of the referencing element does not need to be the same as a [BirDatabase] of the referenced element.
 *
 * Example:
 * ```kotlin
 * val variableReads = registerBackReferencesKey(BirGetValue) { it.symbol.owner }
 * val birVariable: BirVariable = // ...
 * birVariable.getBackReferences(variableReads).forEach {
 *    assert(it is BirGetValue)
 *    assert(it.symbol.owner == birVariable)
 * }
 * ```
 */
fun <E : BirElement, R : BirElement> R.getBackReferences(key: BirElementBackReferencesKey<E, R>): List<E> {
    @Suppress("UNCHECKED_CAST")
    return (this as BirElementBase).getBackReferences(key) as List<E>
}