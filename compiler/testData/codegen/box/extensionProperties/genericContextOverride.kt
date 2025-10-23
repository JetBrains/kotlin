// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// MODULE: m1
// FILE: Base.kt
abstract class Base<T> {
    context(_: T)
    abstract val String.foo: Int?

    context(_: T)
    abstract fun foo(): Int?
}

// MODULE: box(m1)
// FILE: box.kt
class Child : Base<String>() {
    context(_: String)
    override val String.foo: Int? get() = 1

    context(_: String)
    override fun foo(): Int? = 1
}

fun box() = "OK"