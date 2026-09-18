// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-84904

// MODULE: lib
// FILE: lib.kt

package lib

value class Point(val x: Int, val y: Int)

class PointHolder(var point: Point)

fun makePoint(x: Int, y: Int): Point = Point(x, y)

fun roundTripPointArray(points: Array<Point>): Array<Point> = points

fun roundTripPointList(points: List<Point>): List<Point> = points

fun pointWithDefaults(
    x: Int = 6,
    point: Point = Point(x, x + 1)
): Point = point

// MODULE: main(lib)
// FILE: main.kt

import lib.*

fun box(): String {
    val point = makePoint(1, 2)

    val array = roundTripPointArray(
        arrayOf(point, Point(3, 4))
    )
    if (!array.contentEquals(arrayOf(Point(1, 2), Point(3, 4)))) {
        return "FAIL array"
    }

    val list = roundTripPointList(
        listOf(point, Point(5, 6))
    )
    if (list != listOf(Point(1, 2), Point(5, 6))) {
        return "FAIL list"
    }

    if (selectPoint(true) != Point(7, 8)) {
        return "FAIL when local branch"
    }

    if (selectPoint(false) != Point(9, 10)) {
        return "FAIL when cross-module branch"
    }

    if (nullablePoint(true) != Point(13, 14)) {
        return "FAIL nullable value"
    }

    if (nullablePoint(false) != null) {
        return "FAIL nullable null"
    }

    if (pointFromTry(false) != Point(15, 16)) {
        return "FAIL try"
    }

    if (pointFromTry(true) != Point(17, 18)) {
        return "FAIL catch"
    }

    val holder = PointHolder(point)
    holder.point = Point(4, 5)
    if (holder.point != Point(4, 5)) {
        return "FAIL mutable property"
    }

    if (pointWithDefaults() != Point(6, 7)) {
        return "FAIL default adapter"
    }

    return "OK"
}

private fun selectPoint(local: Boolean): Point =
    when (local) {
        true -> Point(7, 8)
        false -> makePoint(9, 10)
    }

private fun nullablePoint(present: Boolean): Point? =
    if (present) makePoint(13, 14) else null

private fun pointFromTry(shouldThrow: Boolean): Point =
    try {
        if (shouldThrow) throw IllegalStateException()
        makePoint(15, 16)
    } catch (_: IllegalStateException) {
        Point(17, 18)
    }
