// LANGUAGE: +FullValueClasses

// MODULE: context
// FILE: context.kt
value class Point(val x: Int, val y: Int) {
    val sum: Int get() = x + y
}

fun test(point: Point, nullablePoint: Point?) {
    <caret_context>Unit
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
val other = Point(point.y, nullablePoint?.x ?: point.x)
if (point == other) point.sum else other.sum
