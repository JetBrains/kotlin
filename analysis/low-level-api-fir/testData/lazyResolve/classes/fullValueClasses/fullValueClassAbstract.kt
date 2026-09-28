// LANGUAGE: +FullValueClasses
package pack

abstract value class Ba<caret>se<T> {
    abstract val first: T

    fun firstValue(): T = first
}
