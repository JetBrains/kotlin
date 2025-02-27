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

import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import java.util.*

abstract class IrElementBase : IrElement {
    override fun <D> transform(transformer: IrTransformer<D>, data: D): IrElement =
        accept(transformer, data)

    override fun <D> acceptChildren(visitor: IrVisitor<Unit, D>, data: D) {
        // No children by default
    }

    override fun <D> transformChildren(transformer: IrTransformer<D>, data: D) {
        // No children by default
    }

    final override var attributeOwnerId: IrElement
        get() = _attributeOwnerId ?: this
        set(value) {
            _attributeOwnerId = value
        }

    private var storage: Array<Any?>? = null
    private var indexAttributesSet: Long = 0

    internal fun <T> getAttributeInternal(attribute: IrAttribute<T>): T? = when (attribute) {
        is IrIndexBasedAttribute<T> -> getAttributeInternal(attribute)
        is IrKeyBasedAttribute<*, T> -> getAttributeInternal(@Suppress("UNCHECKED_CAST") (attribute as IrKeyBasedAttribute<*, T & Any>))
        is IrIndexBasedFlag -> @Suppress("UNCHECKED_CAST") (getFlagInternal(attribute) as T)
    }

    internal fun <T> setAttributeInternal(attribute: IrAttribute<T>, value: T?) = when (attribute) {
        is IrIndexBasedAttribute<T> -> setAttributeInternal(attribute, value)
        is IrKeyBasedAttribute<*, T> -> setAttributeInternal(@Suppress("UNCHECKED_CAST") (attribute as IrKeyBasedAttribute<*, T & Any>), value)
        is IrIndexBasedFlag -> setFlagInternal(attribute, value as Boolean)
    }

    protected fun preallocateStorage(size: Int) {
        storage = arrayOfNulls<Any?>(size)
    }

    fun removeAllAttributes() {
        storage?.fill(null)
        indexAttributesSet = 0
    }

    val attributes: Map<IrAttribute<*>, Any>
        get() = indexBasedAttributes + keyBasedAttributes


    @PublishedApi
    internal fun <T> getAttributeInternal(attribute: IrIndexBasedAttribute<T>): T? {
        val storage = storage ?: return null

        val attributesSet = indexAttributesSet
        if (attributesSet and attribute.bitMask == 0L) return null

        val prefixMask = attributesSet and attribute.prefixMask
        val index = prefixMask.countOneBits()

        @Suppress("UNCHECKED_CAST")
        return storage[index] as T
    }

    @PublishedApi
    internal fun <T> setAttributeInternal(attribute: IrIndexBasedAttribute<T>, value: T?) {
        var storage = storage
        if (storage == null) {
            initStorageWithOneIndexAttribute(attribute, value)
            return
        }

        val attributesSet = indexAttributesSet
        val prefixMask = attributesSet and attribute.prefixMask
        val index = prefixMask.countOneBits()

        val attributeMask = attribute.bitMask
        if (attributesSet and attributeMask == 0L) {
            if (value == null) {
                return
            }

            val indexBasedAttributes = (attributesSet and ATTRIBUTES_MASK).countOneBits()
            if (indexBasedAttributes == storage.size || storage[indexBasedAttributes] != null) {
                val delta = 4
                val newStorage = arrayOfNulls<Any?>(storage.size + delta)
                storage.copyInto(newStorage, 0, 0, index)
                storage.copyInto(newStorage, index + 1, index, indexBasedAttributes)
                storage.copyInto(newStorage, indexBasedAttributes + delta, indexBasedAttributes, storage.size)

                storage = newStorage
                this.storage = newStorage
            } else {
                storage.copyInto(storage, index + 1, index, indexBasedAttributes)
            }

            storage[index] = value
            indexAttributesSet = attributesSet or attributeMask
        } else {
            if (value == null) {
                val indexBasedAttributes = (attributesSet and ATTRIBUTES_MASK).countOneBits()
                if (index < indexBasedAttributes - 1) {
                    storage.copyInto(storage, index, index + 1, indexBasedAttributes)
                }
                storage[indexBasedAttributes - 1] = null
                indexAttributesSet = attributesSet xor attributeMask
            } else {
                storage[index] = value
            }
        }
    }

    private fun <T> initStorageWithOneIndexAttribute(attribute: IrIndexBasedAttribute<T>, value: T?) {
        if (value == null) {
            return
        }

        val storage = arrayOfNulls<Any?>(4)
        storage[0] = value
        this.storage = storage

        indexAttributesSet = attribute.bitMask
    }

    protected fun <T> initAttribute(attribute: IrIndexBasedAttribute<T>, value: T) {
        if (value == null) {
            return
        }

        val attributesSet = indexAttributesSet
        val prefixMask = attributesSet and attribute.prefixMask
        val index = prefixMask.countOneBits()

        storage!![index] = value
        indexAttributesSet = attributesSet or attribute.bitMask
    }

    @PublishedApi
    internal fun getFlagInternal(attribute: IrIndexBasedFlag): Boolean {
        return indexAttributesSet and attribute.bitMask != 0L
    }

    @PublishedApi
    internal fun setFlagInternal(attribute: IrIndexBasedFlag, value: Boolean) {
        indexAttributesSet = if (value) indexAttributesSet or attribute.bitMask
        else indexAttributesSet and attribute.bitMask.inv()
    }

    val indexBasedAttributes: Map<IrIndexBasedAttributeBase<*>, Any>
        get() {
            var mask = indexAttributesSet and ATTRIBUTES_MASK
            if (mask == 0L) return emptyMap()
            return buildMap(mask.countOneBits()) {
                val storage = storage!!
                var lastId = 0
                var valueIndex = 0
                while (mask != 0L) {
                    val delta = mask.countTrailingZeroBits()
                    val id = lastId + delta
                    lastId += delta + 1
                    mask = mask ushr (delta + 1)

                    val attr = IrIndexBasedAttributeRegistry.getById(this.javaClass, id)
                    val value = when (attr) {
                        is IrIndexBasedAttribute<*> -> storage[valueIndex++]!!
                        is IrIndexBasedFlag -> true
                    }
                    put(attr, value)
                }
            }
        }


    internal fun <T : Any> getAttributeInternal(attribute: IrKeyBasedAttribute<*, T>): T? {
        val storage = storage ?: return null

        val low = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits() - 1
        val high = storage.size - 2
        var i = high
        while (i > low) {
            val foundKey = storage[i] ?: break
            if (foundKey === attribute) {
                @Suppress("UNCHECKED_CAST")
                return storage[i + 1] as T
            }

            i -= 2
        }

        return null
    }

    internal fun <T : Any> setAttributeInternal(attribute: IrKeyBasedAttribute<*, T>, value: T?) {
        val storage = storage
        if (storage == null) {
            initStorageWithOneKeyAttribute(attribute, value)
            return
        }

        var found = false
        val low = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits() - 1
        var index = storage.size - 2
        while (index > low) {
            val foundKey = storage[index] ?: break
            if (foundKey === attribute) {
                found = true
                break
            }

            index -= 2
        }

        if (found) {
            if (value == null) {
                removeAttributeAt(index)
            } else {
                storage[index + 1] = value
            }
        } else if (value != null) {
            addAttributeAt(index, attribute, value)
        }
    }

    private fun <T : Any> initStorageWithOneKeyAttribute(attribute: IrKeyBasedAttribute<*, T>, value: T?) {
        if (value == null) {
            return
        }

        val initialSlots = 1
        val storage = arrayOfNulls<Any?>(initialSlots * 2)
        storage[0] = attribute
        storage[1] = value
        this.storage = storage
    }

    private fun <T : Any> addAttributeAt(index: Int, attribute: IrKeyBasedAttribute<*, T>, value: T) {
        var storage = storage!!

        val indexBasedAttributes = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits()
        var newIndex = index
        if (index < indexBasedAttributes) {
            val newSlots = 2
            val delta = newSlots * 2
            newIndex += delta

            val newStorage = arrayOfNulls<Any?>(storage.size + delta)
            storage.copyInto(newStorage, 0, 0, indexBasedAttributes)
            storage.copyInto(newStorage, indexBasedAttributes + delta, indexBasedAttributes, storage.size)

            storage = newStorage
            this.storage = newStorage
        }

        storage[newIndex] = attribute
        storage[newIndex + 1] = value
    }

    private fun removeAttributeAt(keyIndex: Int) {
        val storage = storage!!

        val low = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits()
        var firstKeyIndex = keyIndex
        for (i in keyIndex - 2 downTo low step 2) {
            if (storage[i] == null) break
            firstKeyIndex = i
        }

        if (firstKeyIndex != keyIndex) {
            storage[keyIndex] = storage[firstKeyIndex]
            storage[keyIndex + 1] = storage[firstKeyIndex + 1]
        }
        storage[firstKeyIndex] = null
        storage[firstKeyIndex + 1] = null
    }

    internal fun copyKeyBasedAttributesFrom(other: IrElementBase, includeAll: Boolean) {
        val srcStorage = other.storage ?: return
        var dstStorage = storage

        val srcIndexBasedAttributes = (other.indexAttributesSet and ATTRIBUTES_MASK).countOneBits()
        val dstIndexBasedAttributes = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits()

        val mergedAttributes = IdentityHashMap<IrKeyBasedAttribute<*, *>, Any?>(
            (srcStorage.size - srcIndexBasedAttributes + (dstStorage?.size ?: 0) - dstIndexBasedAttributes) / 2
        )

        if (dstStorage != null) {
            for (i in dstStorage.size - 2 downTo dstIndexBasedAttributes step 2) {
                val attr = dstStorage[i] as IrKeyBasedAttribute<*, *>? ?: break
                mergedAttributes[attr] = dstStorage[i + 1]
            }
        }
        for (i in srcStorage.size - 2 downTo srcIndexBasedAttributes step 2) {
            val attr = srcStorage[i] as IrKeyBasedAttribute<*, *>? ?: break
            if (attr.copyByDefault || includeAll) {
                mergedAttributes[attr] = srcStorage[i + 1]
            }
        }

        if (mergedAttributes.isEmpty()) {
            return
        }

        val targetStorageSize = mergedAttributes.size * 2 + dstIndexBasedAttributes
        if (dstStorage == null || dstStorage.size < targetStorageSize) {
            dstStorage = dstStorage?.copyOf(targetStorageSize) ?: arrayOfNulls(targetStorageSize)
            this.storage = dstStorage
        }

        var i = dstStorage.size - 2
        for ((attr, value) in mergedAttributes) {
            dstStorage[i] = attr
            dstStorage[i + 1] = value
            i -= 2
        }
    }

    val keyBasedAttributes: Map<IrKeyBasedAttribute<*, *>, Any>
        get() {
            val storage = this.storage
                ?: return emptyMap()
            val indexBasedAttributes = (indexAttributesSet and ATTRIBUTES_MASK).countOneBits()
            return buildMap((storage.size - indexBasedAttributes) / 2) {
                for (i in storage.size - 2 downTo indexBasedAttributes step 2) {
                    val key = storage[i] as IrKeyBasedAttribute<*, *>?
                        ?: break
                    val value = storage[i + 1]!!
                    put(key, value)
                }
            }
        }


    companion object {
        private const val MAX_FLAG_ATTRIBUTES = 10
        private const val ATTRIBUTES_MASK = -1L ushr MAX_FLAG_ATTRIBUTES
    }
}
