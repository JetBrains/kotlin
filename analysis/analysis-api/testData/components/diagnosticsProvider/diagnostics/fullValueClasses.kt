// LANGUAGE: +FullValueClasses

value class MutableProperty(var x: Int, val y: Int)

value class StoredProperty(val x: Int, val y: Int) {
    val sum: Int = x + y
}

value class Point(val x: Int, val y: Int)

fun same(first: Point, second: Point): Boolean = first === second

value object Empty

fun sameObject(first: Empty, second: Empty): Boolean = first === second
