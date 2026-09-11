// LANGUAGE: +FullValueClasses
value class Point(val x: Int, val y: Int) {
    constructor(value: Int) : this(value, value)

    val sum: Int get() = x + y

    fun scaled(factor: Int): Point = Point(x * factor, y * factor)
}
