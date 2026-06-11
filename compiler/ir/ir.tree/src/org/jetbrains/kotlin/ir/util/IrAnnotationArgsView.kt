/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.name.Name

class IrAnnotationArgsView(
    private val original: ArrayList<IrExpression?>,
    private val symbol: IrConstructorSymbol
) : Map<Name, IrExpression?> {
    private class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>

    override val keys: Set<Name>
        get() = symbol.owner.parameters.map { it.name }.toSet()

    override val values: Collection<IrExpression?>
        get() = original

    override val entries: Set<Map.Entry<Name, IrExpression?>>
        get() = keys.zip(values).map { Entry(it.first, it.second) }.toSet()

    override val size: Int
        get() = original.size

    override fun get(key: Name): IrExpression? {
        val parameters = symbol.owner.parameters
        val parameter = parameters.firstOrNull { it.name == key } ?: return null
        return original[parameter.indexInParameters]
    }

    override fun isEmpty(): Boolean {
        return original.isEmpty()
    }

    override fun containsKey(key: Name): Boolean {
        val parameters = symbol.owner.parameters
        return parameters.any { it.name == key }
    }

    override fun containsValue(value: IrExpression?): Boolean {
        return original.contains(value)
    }
}
