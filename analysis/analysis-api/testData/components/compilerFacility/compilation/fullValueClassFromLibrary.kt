// LANGUAGE: +FullValueClasses

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

value class Point(val x: Int, val y: Int = 0) {
    val sum: Int get() = x + y
}

inline fun Point.shift(dx: Int, dy: Int): Point = Point(x + dx, y + dy)

// MODULE: main(lib)
// FILE: main.kt
import test.Point
import test.shift

fun test(point: Point): Point = Point(point.sum).shift(1, 2)
