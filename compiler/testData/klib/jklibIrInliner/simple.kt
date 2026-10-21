// MODULE: lib
// FILE: lib.kt
package lib

inline fun double(x: Int): Int = x * 2

// MODULE: main(lib)
// FILE: main.kt
package test

import lib.double

inline fun triple(x: Int): Int = x * 3

fun test(): Int = double(21) + triple(14)
