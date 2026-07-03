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
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.org.objectweb.asm.Type

class IrFrameMap {
    private val typeMap = mutableMapOf<IrSymbol, Type>()

    private val myVarIndex = Object2IntOpenHashMap<IrSymbol>()
    private val myVarSizes = Object2IntOpenHashMap<IrSymbol>()
    var currentSize = 0
        private set

    fun enter(declaration: IrSymbolOwner, type: Type): Int {
        val key = declaration.symbol
        typeMap[key] = type
        val index = currentSize
        myVarIndex.put(key, index)
        currentSize += type.size
        myVarSizes.put(key, type.size)
        return index
    }

    fun leave(key: IrSymbol): Int {
        typeMap.remove(key)
        val size = myVarSizes.getValue(key)
        currentSize -= size
        myVarSizes.removeInt(key)
        val oldIndex = myVarIndex.removeInt(key)
        if (oldIndex != currentSize) {
            throw IllegalStateException("Variable can be left only if it is the last: $key")
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

    fun getIndex(symbol: IrSymbol): Int {
        return if (myVarIndex.contains(symbol)) myVarIndex.getInt(symbol) else -1
    }

    fun skipTo(target: Int): Mark {
        return Mark(currentSize).also {
            if (currentSize < target)
                currentSize = target
        }
    }

    inner class Mark(private val myIndex: Int) {
        fun dropTo() {
            val variablesToDrop = ArrayList<IrSymbol>()
            val iterator = myVarIndex.object2IntEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val [key, value] = iterator.next()
                if (value >= myIndex) {
                    variablesToDrop.add(key)
                }
            }
            for (symbol in variablesToDrop) {
                myVarIndex.removeInt(symbol)
                myVarSizes.removeInt(symbol)
            }
            currentSize = myIndex
        }
    }

    fun typeOf(symbol: IrSymbol): Type = typeMap[symbol]
        ?: error("No mapping for symbol: ${symbol.owner.render()}")

    override fun toString(): String {
        val sb = StringBuilder()

        if (myVarIndex.size != myVarSizes.size) {
            return "inconsistent"
        }

        val symbols = mutableListOf<Triple<IrSymbol, Int, Int>>()

        for (symbol in myVarIndex.keys) {
            val varIndex = myVarIndex.getInt(symbol)
            val varSize = myVarSizes.getInt(symbol)
            symbols.add(Triple(symbol, varIndex, varSize))
        }

        symbols.sortBy { left -> left.second }

        sb.append("size=").append(currentSize)

        var first = true
        for ([symbol, varIndex, varSize] in symbols) {
            if (!first) {
                sb.append(", ")
            }
            first = false
            sb.append(symbol).append(",i=").append(varIndex).append(",s=").append(varSize)
        }

        return sb.toString()
    }
}
