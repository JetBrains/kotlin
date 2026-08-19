// LANGUAGE: +FullValueClasses

abstract value class Base<T> {
    abstract val value: T

    fun get(): T = value
}

value class Derived(override val value: String, val count: Int) : Base<String>()

fun test(derived: Derived) {
    <expr>derived.get()</expr>
}
