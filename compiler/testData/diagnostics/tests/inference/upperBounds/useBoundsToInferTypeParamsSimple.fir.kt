// !WITH_NEW_INFERENCE
package a

fun <V: U, U> foo(v: V, u: U) = u
fun <U, V: U> bar(v: V, u: U) = u

fun test(a: Any, s: String) {
    val b = foo(a, s)
    checkItIsExactlyAny(a, arrayListOf(b))
    val c = bar(a, s)
    checkItIsExactlyAny(a, arrayListOf(c))
}

fun <T> checkItIsExactlyAny(t: T, l: MutableList<T>) {}

fun <V : U, U> baz(v: V, u: MutableSet<U>) = u

fun test(a: Any, s: MutableSet<String>) {
    <!INAPPLICABLE_CANDIDATE!>baz<!>(a, s)
}

//from standard library
fun <T> arrayListOf(vararg t: T): MutableList<T> {}
