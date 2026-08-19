// LANGUAGE: +FullValueClasses
// callable: test/FullValueClass.value
package test

interface HasValue<T> {
    val value: T
}

abstract value class Base<T> : HasValue<T> {
    abstract override val value: T
}

value class FullValueClass(override val value: String, val count: Int) : Base<String>()
