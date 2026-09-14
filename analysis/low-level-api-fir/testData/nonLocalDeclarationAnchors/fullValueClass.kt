// LANGUAGE: +FullValueClasses
package one

value class Point(val x: Int, val y: Int) {
    constructor(value: Int) : this(value, value)

    init {
        val total = x + y
    }

    val sum: Int get() = x + y

    fun scaled(factor: Int): Point = Point(x * factor, y * factor)
}

value object Empty
