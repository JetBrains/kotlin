/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrElementBase : IrElement {
    /**
     * The array stores dense pairs of keys and values, followed by remaining nulls.
     * This is, the layout may look like this: `[key, value, key, value, null, null, null, ...]`
     * Keys are of type [IrAttribute].
     * Values are arbitrary objects but cannot be null.
     */
    private var attributeMap: Array<Any?>? = null


    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement =
        accept(transformer, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children by default
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // No children by default
    }


    /**
     * Returns a snapshot of all attributes held by this element.
     * Designated mainly for debugging.
     */
    val allAttributes: Map<IrAttribute<*, *>, Any>
        get() {
            val attributeMap = attributeMap
                ?: return emptyMap()
            return buildMap(attributeMap.size / 2) {
                for (i in attributeMap.indices step 2) {
                    val key = attributeMap[i] as IrAttribute<*, *>?
                        ?: break
                    val value = attributeMap[i + 1]!!
                    put(key, value)
                }
            }
        }

    internal fun <T : Any> getAttributeInternal(key: IrAttribute<*, T>): T? {
        val foundIndex = findAttributeIndex(key)
        if (foundIndex < 0) {
            return null
        } else {
            @Suppress("UNCHECKED_CAST")
            return attributeMap!![foundIndex + 1] as T
        }
    }

    internal fun <T : Any> setAttributeInternal(key: IrAttribute<*, T>, value: T?): T? {
        val foundIndex = findAttributeIndex(key)
        val previousValue: T? = if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            attributeMap!![foundIndex + 1] as T
        } else null

        putAttribute(foundIndex, key, value)
        return previousValue
    }

    private fun findAttributeIndex(key: IrAttribute<*, *>): Int {
        val attributeMap = attributeMap
            ?: return -1

        var i = 0
        while (i < attributeMap.size) {
            val foundKey = attributeMap[i]
                ?: break
            if (foundKey === key) {
                return i
            }

            i += 2
        }
        return i.inv()
    }

    private fun <T : Any> initializeAttributes(firstKey: IrAttribute<*, T>, firstValue: T) {
        val initialSlots = 1
        val attributeMap = arrayOfNulls<Any?>(initialSlots * 2)
        attributeMap[0] = firstKey
        attributeMap[1] = firstValue
        this.attributeMap = attributeMap
    }

    private fun <T : Any> putAttribute(existingIndex: Int, key: IrAttribute<*, T>, value: T?) {
        if (existingIndex >= 0) {
            if (value == null) {
                removeAttributeAt(existingIndex)
            } else {
                attributeMap!![existingIndex + 1] = value
            }
        } else if (value != null) {
            if (attributeMap == null) {
                initializeAttributes(key, value)
            } else {
                val newEntryIndex = existingIndex.inv()
                addAttributeAt(newEntryIndex, key, value)
            }
        }
    }

    private fun <T : Any> addAttributeAt(index: Int, key: IrAttribute<*, T>, value: T) {
        var attributeMap = attributeMap!!
        if (attributeMap.size <= index) {
            val newSlots = 2
            attributeMap = attributeMap.copyOf(attributeMap.size + newSlots * 2)
            this.attributeMap = attributeMap
        }

        attributeMap[index] = key
        attributeMap[index + 1] = value
    }

    private fun removeAttributeAt(keyIndex: Int) {
        // It is expected that during the compilation process, attributes are mostly appended
        // and rarely removed, hence no need to shrink the array.

        val attributeMap = attributeMap!!

        var lastKeyIndex = attributeMap.size - 2
        while (lastKeyIndex > keyIndex && attributeMap[lastKeyIndex] == null) {
            lastKeyIndex -= 2
        }

        if (lastKeyIndex > keyIndex) {
            attributeMap[keyIndex] = attributeMap[lastKeyIndex]
            attributeMap[keyIndex + 1] = attributeMap[lastKeyIndex + 1]
        }
        attributeMap[lastKeyIndex] = null
        attributeMap[lastKeyIndex + 1] = null
    }
}
