// pack.Point
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

abstract value class Base<T>(parameter: T) {
    abstract val first: T

    open fun describe(): String = "Base"
    fun size(): Int = 1
}

value class Point<T>(override val first: T, val second: T) : Base<T>(first) {
    constructor(value: T) : this(value, value)

    override fun describe(): String = "Point"

    val last: T get() = second
}
