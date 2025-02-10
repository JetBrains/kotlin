/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import kotlin.reflect.KMutableProperty1

data class Model<Element : AbstractElement<Element, *, *>>(
    val elements: List<Element>,
    val rootElement: Element,
) {
    fun inheritFields() {
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

    fun addPureAbstractElement(pureAbstractElement: ClassRef<*>) {
        for (el in elements) {
            if (el.needPureAbstractElement) {
                el.otherParents.add(pureAbstractElement)
            }
        }
    }

    fun specifyHasAcceptAndTransformChildrenMethods() {
        for (el in elements) {
            el.hasAcceptChildrenMethod = el.isRootElement || (el.subElements.isEmpty() && el.walkableChildren.isNotEmpty())
            el.hasTransformChildrenMethod = el.isRootElement || (el.subElements.isEmpty() && el.transformableChildren.isNotEmpty())
        }

        rootElement.elementDescendantsAndSelfDepthFirst().toList().reversed().forEach { element ->
            @Suppress("UNCHECKED_CAST")
            bubbleUpAcceptOrTransformChildrenMethod(
                element,
                AbstractElement<*, *, *>::hasAcceptChildrenMethod as KMutableProperty1<Element, Boolean>,
                AbstractElement<*, *, *>::walkableChildren
            )
            @Suppress("UNCHECKED_CAST")
            bubbleUpAcceptOrTransformChildrenMethod(
                element,
                AbstractElement<*, *, *>::hasTransformChildrenMethod as KMutableProperty1<Element, Boolean>,
                AbstractElement<*, *, *>::transformableChildren
            )
        }
    }

    private fun bubbleUpAcceptOrTransformChildrenMethod(
        parent: Element,
        hasAcceptOrTransformChildrenMethod: KMutableProperty1<Element, Boolean>,
        walkableOrTransformableChildren: Element.() -> List<AbstractField<*>>,
    ) {
        if (parent.subElements.isNotEmpty() &&
            parent.subElements.all {
                hasAcceptOrTransformChildrenMethod.get(it) &&
                        it.walkableOrTransformableChildren().size == parent.walkableOrTransformableChildren().size
                        && it.childrenOrderOverride == null
            }
        ) {
            for (child in parent.subElements) {
                hasAcceptOrTransformChildrenMethod.set(child, false)
            }

            hasAcceptOrTransformChildrenMethod.set(parent, true)
        }
    }
}
