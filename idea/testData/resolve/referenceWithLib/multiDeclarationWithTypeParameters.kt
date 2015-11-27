package test

import dependency.*

fun <Int> f(l: List<Int>) {
    val (e<caret>l1, el2, el3) = l
}

// MULTIRESOLVE
// REF: (for kotlin.collections.List<T> in dependency).component1()
// REF: (for kotlin.collections.List<T> in dependency).component2()
// REF: (for kotlin.collections.List<T> in dependency).component3()

