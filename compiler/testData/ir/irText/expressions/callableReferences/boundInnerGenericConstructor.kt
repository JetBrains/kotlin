// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57433, KT-57429

package test

class Foo<T> {
    inner class Inner<P>(val a: T, val b: P)
}

inline fun <A, B> foo(a: A, b: B, x: (A, B) -> Foo<A>.Inner<B>): Foo<A>.Inner<B> = x(a, b)

fun box(): String {
    val z = Foo<String>()
    val foo = foo("O", "K", z::Inner)
    return foo.a + foo.b
}
