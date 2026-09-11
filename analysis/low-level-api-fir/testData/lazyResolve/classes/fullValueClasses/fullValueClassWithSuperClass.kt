// LANGUAGE: +FullValueClasses
package pack

abstract value class Base<T> {
    abstract val first: T

    fun firstValue(): T = first
}

value class Full<caret>ValueClass<T>(override val first: T, val second: T) : Base<T>()
