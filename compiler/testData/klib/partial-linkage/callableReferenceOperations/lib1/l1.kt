@file:Suppress("NOTHING_TO_INLINE")

fun removedFun(x: Int): Int = x
inline fun removedInlineFun(x: Int): Int = x

class ClassWithRemovedCtor {
    constructor(x: Int) {}
}

fun removedGetRegularClassInstance(): RegularClass = RegularClass()
class RegularClass {
    fun foo(): Int = 312
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