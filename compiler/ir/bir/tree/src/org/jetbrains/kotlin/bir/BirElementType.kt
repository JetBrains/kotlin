/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

sealed class BirElementType<out T : BirElement> {
    abstract val possibleClasses: Set<BirElementClass<out T>>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BirElementType<*>) return false

        fun checkUnion(a: BirElementType<*>, b: BirElementType<*>): Boolean {
            if (a is BirElementUnionType<*>) {
                return when (b) {
                    is BirElementUnionType<*> -> a.options == b.options
                    is BirElementClass<*> -> a.options.singleOrNull() == b
                }
            }
            return false
        }

        if (checkUnion(this, other)) return true
        if (checkUnion(other, this)) return true

        return false
    }

    override fun hashCode(): Int {
        when (this) {
            is BirElementUnionType<*> -> {
                options.singleOrNull()?.let {
                    return it.hashCode()
                }
                return options.hashCode()
            }
            is BirElementClass<*> -> return super.hashCode()
        }
    }
}

