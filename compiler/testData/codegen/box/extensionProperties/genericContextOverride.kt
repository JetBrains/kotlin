// !LANGUAGE: +ContextReceivers
// MODULE: m1
// FILE: Base.kt
abstract class Base<T> {
    context(T)
    abstract val String.foo: Int?

    context(T)
    abstract fun foo(): Int?
}

// MODULE: box(m1)
// FILE: box.kt
class Child : Base<String>() {
    context(String)
    override val String.foo: Int? get() = 1

    context(String)
    override fun foo(): Int? = 1
}

fun box() = "OK"