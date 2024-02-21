/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

internal fun <Element : AbstractElement<Element, *, *>> initializeSubElements(elements: List<Element>){
    val elementSubclasses = elements.associateWith { mutableSetOf<Element>() }

    for (element in elements) {
        for (parent in element.elementParents) {
            elementSubclasses.getValue(parent.element) += element
        }
    }

    for ((element, subElements) in elementSubclasses) {
        element.subElements = subElements
    }
}
