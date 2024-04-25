// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

@DslMarker
annotation class MyDsl

@MyDsl
interface Scope<A, B> {
    val something: A
    val value: B
}
fun scoped1(block: Scope<Int, String>.() -> Unit) {}
fun scoped2(block: Scope<*, String>.() -> Unit) {}

val <T> Scope<*, T>.property: T get() = value

fun f() {
    scoped1 {
        value
        property
    }
    scoped2 {
        value
        property
    }
}