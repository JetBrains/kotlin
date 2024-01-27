/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

class BirElementUnionType<out T : BirElement>(
    options: Set<BirElementType<T>>,
) : BirElementType<T>() {
    override val possibleClasses: Set<BirElementClass<out T>>
    val options: Set<BirElementType<T>>
        get() = possibleClasses

    init {
        require(options.isNotEmpty())

        val flatList = LinkedHashSet<BirElementClass<T>>(options.size)
        fun addAll(types: Set<BirElementType<T>>) {
            for (type in types) {
                when (type) {
                    is BirElementUnionType<T> -> addAll(type.options)
                    is BirElementClass<T> -> flatList += type
                }
            }
        }
        addAll(options)
        this.possibleClasses = flatList
    }

    override fun toString(): String {
        return options.joinToString(" | ")
    }
}

infix fun <T : BirElement> BirElementType<T>.or(other: BirElementType<T>): BirElementUnionType<T> {
    val options = setOf(this, other)
    return BirElementUnionType<T>(options)
}