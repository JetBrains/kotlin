// LANGUAGE: +FullValueClasses
// ISSUE: KT-84904

// MODULE: lib
// FILE: lib.kt

package lib

value class Point(val x: Int, val y: Int)

inline fun transform(point: Point, operation: (Point) -> Point): Point = operation(point)

// MODULE: main(lib)
// FILE: main.kt

import lib.*

fun box(): String {
    val result = transform(Point(1, 2)) { point ->
        Point(point.x + 3, point.y + 4)
    }

    return if (result == Point(4, 6)) "OK" else "FAIL"
}
