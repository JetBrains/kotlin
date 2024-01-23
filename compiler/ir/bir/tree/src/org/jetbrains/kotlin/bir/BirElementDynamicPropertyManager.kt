/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirElementDynamicPropertyManager {
    private val elementClasses = arrayOfNulls<ElementClassData>(BirMetadata.allElements.maxOf { it.id } + 1)
    private var totalTokenRegistrations = 0

    fun <E : BirElement, T> acquireProperty(key: BirElementDynamicPropertyKey<E, T>): BirElementDynamicPropertyToken<E, T> {
        val classData = getElementClassData(key.elementType.id)

        refreshKeysFromAncestors(classData)
        classData.keys += key
        classData.keyCount = classData.keys.size
        if (key.id == -1) {
            key.id = classData.keyCount - 1
        }
        totalTokenRegistrations++

        return BirElementDynamicPropertyToken(this, key)
    }

    private fun getElementClassData(elementClassId: Int): ElementClassData {
        val data = elementClasses[elementClassId]
        if (data?.ancestorElements != null) {
            return data
        }

        return computeElementClassData(elementClassId, data)
    }

    private fun computeElementClassData(elementClassId: Int, currentData: ElementClassData?): ElementClassData {
        var data = currentData

        val elementClass = BirMetadata.allElementsById[elementClassId]!!
        if (data == null) {
            data = ElementClassData(elementClass)
            elementClasses[elementClassId] = data
        }

        val ancestorElements = mutableSetOf<ElementClassData>()
        fun visitParents(javaClass: Class<*>) {
            if (javaClass != elementClass.javaClass) {
                val clazz = BirMetadata.allElementsByJavaClass[javaClass] ?: return
                val elementClassData = getElementClassData(clazz.id)
                if (!ancestorElements.add(elementClassData)) {
                    return
                }
            }

            javaClass.superclass?.let {
                visitParents(it)
            }
            javaClass.interfaces.forEach {
                visitParents(it)
            }
        }
        visitParents(elementClass.javaClass)

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

    internal fun getInitialDynamicPropertyArraySize(elementClassId: Int): Int {
        val data = getElementClassData(elementClassId)
        refreshKeysFromAncestors(data)
        return data.keyCount
    }

    private class ElementClassData(
        val elementClass: BirElementClass<*>,
    ) {
        var ancestorElements: List<ElementClassData>? = null
        val keys = mutableSetOf<BirElementDynamicPropertyKey<*, *>>()
        var keyCount = 0
        var lastSeenTotalTokenRegistrations = 0

        override fun toString() = elementClass.toString()
    }
}

class BirElementDynamicPropertyKey<E : BirElement, T>(
    internal val elementType: BirElementClass<E>,
) {
    internal var id = -1
}

class BirElementDynamicPropertyToken<E : BirElement, T> internal constructor(
    internal val manager: BirElementDynamicPropertyManager,
    val key: BirElementDynamicPropertyKey<E, T>,
)