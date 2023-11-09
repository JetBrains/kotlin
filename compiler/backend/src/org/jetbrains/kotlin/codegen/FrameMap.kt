/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.org.objectweb.asm.Type

open class FrameMap : FrameMapBase<DeclarationDescriptor>()

open class FrameMapBase<T : Any> {
    private val myVarIndex = Object2IntOpenHashMap<T>()
    private val myVarSizes = Object2IntOpenHashMap<T>()
    var currentSize = 0
        private set

    open fun enter(key: T, type: Type): Int {
        val index = currentSize
        myVarIndex.put(key, index)
        currentSize += type.size
        myVarSizes.put(key, type.size)
        return index
    }

    open fun leave(key: T): Int {
        val size = myVarSizes.getValue(key)
        currentSize -= size
        myVarSizes.removeInt(key)
        val oldIndex = myVarIndex.removeInt(key)
        if (oldIndex != currentSize) {
            throw IllegalStateException("Descriptor can be left only if it is last: $key")
        }
        return oldIndex
    }

    fun enterTemp(type: Type): Int {
        val result = currentSize
        currentSize += type.size
        return result
    }

    fun leaveTemp(type: Type) {
        currentSize -= type.size
    }

    open fun getIndex(descriptor: T): Int {
        return if (myVarIndex.contains(descriptor)) myVarIndex.getInt(descriptor) else -1
    }

    fun mark(): Mark {
        return Mark(currentSize)
    }

    fun skipTo(target: Int): Mark {
        return mark().also {
            if (currentSize < target)
                currentSize = target
        }
    }

    inner class Mark(private val myIndex: Int) {

        fun dropTo() {
            val descriptorsToDrop = ArrayList<T>()
            val iterator = myVarIndex.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val (key, value) = iterator.next()
                if (value >= myIndex) {
                    descriptorsToDrop.add(key)
                }
            }
            for (declarationDescriptor in descriptorsToDrop) {
                myVarIndex.removeInt(declarationDescriptor)
                myVarSizes.removeInt(declarationDescriptor)
            }
            currentSize = myIndex
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        if (myVarIndex.size != myVarSizes.size) {
            return "inconsistent"
        }

        val descriptors = mutableListOf<Triple<T, Int, Int>>()

        for (descriptor0 in myVarIndex.keys) {
            @Suppress("UNCHECKED_CAST") val descriptor = descriptor0 as T
            val varIndex = myVarIndex.getInt(descriptor)
            val varSize = myVarSizes.getInt(descriptor)
            descriptors.add(Triple(descriptor, varIndex, varSize))
        }

        descriptors.sortBy { left -> left.second }

        sb.append("size=").append(currentSize)

        var first = true
        for (t in descriptors) {
            if (!first) {
                sb.append(", ")
            }
            first = false
            sb.append(t.first).append(",i=").append(t.second).append(",s=").append(t.third)
        }

        return sb.toString()
    }
}
