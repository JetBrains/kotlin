// WITH_STDLIB
// IGNORE_BACKEND: JVM, WASM, JS, JS_IR

@JvmInline
value class Inlined(val value: Int)

sealed interface A <T: Inlined> {
    fun foo(i: T?)
}

class B : A<Nothing> {
    override fun foo(i: Nothing?) {}
}

fun box(): String {
    val a: A<*> = B()
    a.foo(null)
    return "OK"
}
