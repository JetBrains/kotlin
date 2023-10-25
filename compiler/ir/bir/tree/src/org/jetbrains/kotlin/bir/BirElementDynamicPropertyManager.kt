/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirElementDynamicPropertyManager {
    private val elementClasses = ElementClassDataMap()
    private var totalTokenRegistrations = 0

    fun <E : BirElement, T> acquireProperty(key: BirElementDynamicPropertyKey<E, T>): BirElementDynamicPropertyToken<E, T> {
        val classData = getElementClassData(key.elementClass)

        refreshKeysFromAncestors(classData)
        classData.keys += key
        classData.keyCount = classData.keys.size
        if (key.index == -1) {
            key.index = classData.keyCount - 1
        }
        totalTokenRegistrations++

        return BirElementDynamicPropertyToken(this, key)
    }

    private fun getElementClassData(elementClass: Class<*>): ElementClassData {
        val data = elementClasses[elementClass]
        if (data.ancestorElements != null) {
            return data
        }

        val ancestorElements = mutableSetOf<ElementClassData>()
        fun visitParents(clazz: Class<*>) {
            if (clazz !== elementClass) {
                if (!BirElement::class.java.isAssignableFrom(clazz)) {
                    return
                }

                if (!ancestorElements.add(getElementClassData(clazz))) {
                    return
                }
            }

            clazz.superclass?.let {
                visitParents(it)
            }
            clazz.interfaces.forEach {
                visitParents(it)
            }
        }
        visitParents(elementClass)

        data.ancestorElements = ancestorElements.toList()
        return data
    }

    private fun refreshKeysFromAncestors(element: ElementClassData) {
        if (element.lastSeenTotalTokenRegistrations < totalTokenRegistrations) {
            for (ancestor in element.ancestorElements!!) {
                element.keys += ancestor.keys
                element.keyCount = element.keys.size
            }
            element.lastSeenTotalTokenRegistrations = totalTokenRegistrations
        }
    }

    internal fun getInitialDynamicPropertyArraySize(elementClass: Class<*>): Int {
        val data = getElementClassData(elementClass)
        refreshKeysFromAncestors(data)
        return data.keyCount
    }

    private class ElementClassData(
        val elementClass: Class<*>,
    ) {
        var ancestorElements: List<ElementClassData>? = null
        val keys = mutableSetOf<BirElementDynamicPropertyKey<*, *>>()
        var keyCount = 0
        var lastSeenTotalTokenRegistrations = 0

        override fun toString() = elementClass.simpleName
    }

    private class ElementClassDataMap : ClassValue<ElementClassData>() {
        override fun computeValue(type: Class<*>): ElementClassData {
            return ElementClassData(type)
        }
    }
}

class BirElementDynamicPropertyKey<E : BirElement, T>(
    internal val elementClass: Class<E>
) {
    internal var index = -1
}

inline fun <reified E : BirElement, T> BirElementDynamicPropertyKey() = BirElementDynamicPropertyKey<E, T>(E::class.java)

class BirElementDynamicPropertyToken<E : BirElement, T> internal constructor(
    internal val manager: BirElementDynamicPropertyManager,
    val key: BirElementDynamicPropertyKey<E, T>
)