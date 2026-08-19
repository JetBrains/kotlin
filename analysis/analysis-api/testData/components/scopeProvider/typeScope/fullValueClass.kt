// LANGUAGE: +FullValueClasses
package test

abstract value class Base<T> {
    abstract val first: T

    fun firstValue(): T = first
}

value class FullValueClass<T>(override val first: T, val second: T) : Base<T>() {
    constructor(value: T) : this(value, value)

    val last: T get() = second

    fun choose(useFirst: Boolean): T = if (useFirst) first else second
}

fun test(value: FullValueClass<String>) {
    <expr>value</expr>
}
