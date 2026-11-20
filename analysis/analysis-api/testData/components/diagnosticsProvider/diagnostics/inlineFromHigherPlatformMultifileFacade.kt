// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_TARGET: 11
// FILE: part1.kt
@file:JvmMultifileClass
@file:JvmName("Facade")
package lib

inline fun inlineFunctionFromPart1() {}

// FILE: part2.kt
@file:JvmMultifileClass
@file:JvmName("Facade")
package lib

inline fun inlineFunctionFromPart2() {}

fun regularFunctionFromPart2() {}

// MODULE: main(lib)
// JVM_TARGET: 1.8
// FILE: main.kt
package main

import lib.*

fun usage() {
    inlineFunctionFromPart1()
    inlineFunctionFromPart2()
    regularFunctionFromPart2()
}
