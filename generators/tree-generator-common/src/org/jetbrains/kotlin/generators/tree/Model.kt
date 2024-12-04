/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

data class Model<Element : AbstractElement<Element, *, *>>(
    val elements: List<Element>,
    val rootElement: Element,
) {
    internal fun inheritFields() {
        val processed = mutableSetOf<Element>()
        fun recurse(element: Element) {
            if (!processed.add(element)) return
            for (parent in element.elementParents) {
                recurse(parent.element)
            }
            element.inheritFields()
        }

        for (element in elements + rootElement) {
            recurse(element)
        }
    }
}
