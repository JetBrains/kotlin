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
    private var dynamicPropertyMap: Array<Any?>? = null


    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement =
        accept(transformer, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // No children by default
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // No children by default
    }


    val allDynamicProperties: Map<IrDynamicPropertyKey<*, *>, Any>
        get() {
            val dynamicPropertyMap = dynamicPropertyMap
                ?: return emptyMap()
            return buildMap(dynamicPropertyMap.size / 2) {
                var i = 0
                while (i < dynamicPropertyMap.size) {
                    val key = dynamicPropertyMap[i] as IrDynamicPropertyKey<*, *>?
                    if (key != null) {
                        val value = dynamicPropertyMap[i + 1]!!
                        put(key, value)
                    }

                    i += 2
                }
            }
        }

    internal open fun <T> getDynamicProperty(key: IrDynamicPropertyKey<*, T>): T? {
        val dynamicPropertyMap = this.dynamicPropertyMap
            ?: return null

        val keyIndex = findDynamicPropertyIndex(dynamicPropertyMap, key)
        if (keyIndex < 0) {
            return null
        } else {
            @Suppress("UNCHECKED_CAST")
            return dynamicPropertyMap[keyIndex + 1] as T
        }
    }

    internal fun <T> setDynamicProperty(key: IrDynamicPropertyKey<*, T>, value: T?): T? {
        val dynamicPropertyMap = this.dynamicPropertyMap
        var previousValue: T? = null
        if (dynamicPropertyMap == null) {
            if (value == null) {
                // Read of any key will return null if the array is null, so no need to initialize it here.
            } else {
                initializeDynamicProperties(key, value)
            }
        } else {
            val foundIndex = findDynamicPropertyIndex(dynamicPropertyMap, key)
            if (foundIndex >= 0) {
                @Suppress("UNCHECKED_CAST")
                previousValue = dynamicPropertyMap[foundIndex + 1] as T

                if (value == null) {
                    removeDynamicPropertyAt(dynamicPropertyMap, foundIndex)
                } else {
                    val valueIndex = foundIndex + 1
                    @Suppress("UNCHECKED_CAST")
                    dynamicPropertyMap[valueIndex] = value
                }
            } else {
                val entryIndex = -(foundIndex + 1)
                addDynamicPropertyAt(dynamicPropertyMap, entryIndex, key, value)
            }
        }

        return previousValue
    }

    internal fun <T> getOrPutDynamicProperty(key: IrDynamicPropertyKey<*, T>, compute: () -> T): T {
        val dynamicPropertyMap = this.dynamicPropertyMap
        val foundIndex = if (dynamicPropertyMap != null)
            findDynamicPropertyIndex(dynamicPropertyMap, key)
        else -1

        if (foundIndex >= 0) {
            @Suppress("UNCHECKED_CAST")
            return dynamicPropertyMap!![foundIndex + 1] as T
        } else {
            val newValue = compute()

            if (dynamicPropertyMap == null) {
                initializeDynamicProperties(key, newValue)
            } else {
                val entryIndex = -(foundIndex + 1)
                addDynamicPropertyAt(dynamicPropertyMap, entryIndex, key, newValue)
            }

            return newValue
        }
    }

    private fun findDynamicPropertyIndex(dynamicPropertyMap: Array<Any?>, key: IrDynamicPropertyKey<*, *>): Int {
        var i = 0
        while (i < dynamicPropertyMap.size) {
            val foundKey = dynamicPropertyMap[i]
            if (foundKey == null) {
                break
            }
            if (foundKey === key) {
                return i
            }

            i += 2
        }
        return -i - 1
    }

    private fun <T> initializeDynamicProperties(firstKey: IrDynamicPropertyKey<*, T>, firstValue: T?) {
        val initialSlots = 1
        val dynamicPropertyMap = arrayOfNulls<Any?>(initialSlots * 2)
        dynamicPropertyMap[0] = firstKey
        dynamicPropertyMap[1] = firstValue
        this.dynamicPropertyMap = dynamicPropertyMap
    }

    private fun <T> addDynamicPropertyAt(dynamicPropertyMap: Array<Any?>, index: Int, key: IrDynamicPropertyKey<*, T>, value: T?): Boolean {
        if (value == null) {
            return false
        }

        var dynamicPropertyMap = dynamicPropertyMap
        if (dynamicPropertyMap.size <= index) {
            val newSlots = 2
            dynamicPropertyMap = dynamicPropertyMap.copyOf(dynamicPropertyMap.size + newSlots * 2)
            this.dynamicPropertyMap = dynamicPropertyMap
        }

        dynamicPropertyMap[index] = key
        dynamicPropertyMap[index + 1] = value
        return true
    }

    private fun removeDynamicPropertyAt(dynamicPropertyMap: Array<Any?>, keyIndex: Int) {
        val lastKeyIndex = dynamicPropertyMap.size - 2
        if (lastKeyIndex > keyIndex) {
            dynamicPropertyMap[keyIndex] = dynamicPropertyMap[lastKeyIndex]
            dynamicPropertyMap[keyIndex + 1] = dynamicPropertyMap[lastKeyIndex + 1]
        }
        dynamicPropertyMap[lastKeyIndex] = null
        dynamicPropertyMap[lastKeyIndex + 1] = null
    }
}
