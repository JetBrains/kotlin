// ISSUE: KT-89995
// MODULE: lib
// MODULE_KIND: LibraryBinary
// JVM_ABI_GEN
// COMPILER_ARGUMENTS: -Xfull-value-classes
// FILE: lib.kt
package lib

value class Point private constructor(val first: Int, val second: Int) {
    companion object {
        fun of(x: Int) = Point(x, x + 1)
    }
}

// MODULE: main(lib)
// LANGUAGE: +FullValueClasses
// FILE: main.kt
import lib.*

fun sum(point: Point) = point.first + point.second

fun test(): Int = sum(Point.of(1))
