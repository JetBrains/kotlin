@file:Suppress("NOTHING_TO_INLINE")

package com.example

class SomeClass {

    @JvmName("changedFunctionJvmName")
    fun changedFunction(): Int = 0

    val changedPropertyAccessor: Int
        @JvmName("changedPropertyAccessorJvmName")
        get() = 0
}

@JvmName("changedInlineFunctionJvmName")
inline fun changedInlineFunction(): Int = 0

inline val changedInlinePropertyAccessor: Int
    @JvmName("changedInlinePropertyAccessorJvmName")
    get() = 0