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
// The consumer doesn't enable FullValueClasses, so it has to skip the pre-release check of the library.
// Remove this test when FullValueClasses is enabled by default.
// LANGUAGE: -FullValueClasses
// COMPILER_ARGUMENTS: -Xskip-prerelease-check
// FILE: main.kt
import lib.*

fun sum(point: Point) = point.first + point.second

fun test(): Int = sum(Point.of(1))
