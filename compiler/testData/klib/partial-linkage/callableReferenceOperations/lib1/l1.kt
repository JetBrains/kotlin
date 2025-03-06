@file:Suppress("NOTHING_TO_INLINE")

fun removedFun(x: Int): Int = x
inline fun removedInlineFun(x: Int): Int = x

class ClassWithRemovedCtor {
    constructor(x: Int) {}
}

val removedVal: Int
    get() = 321
inline val removedInlineVal: Int
    get() = 321

var removedVar: Int
    get() = 321
    set(value) {}
inline var removedInlineVar: Int
    get() = 321
    set(value) {}