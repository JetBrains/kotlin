// LANGUAGE: +FullValueClasses

sealed value class Result {
    abstract val size: Int
}

value class Point(val x: Int, val y: Int = 0) : Result() {
    constructor(other: Point) : this(other.x, other.y)

    override val size: Int get() = x + y
}

value object Empty : Result() {
    override val size: Int get() = 0
}

class RegularResult(override val size: Int) : Result()

value class SingleField(val value: Int)

fun createPoint(x: Int): Point = Point(x)

fun copyPoint(point: Point): Point = Point(point)

fun same(first: Point, second: Point): Boolean = first == second

fun size(result: Result): Int = when (result) {
    is Point -> result.size
    Empty -> 0
    is RegularResult -> result.size
}
