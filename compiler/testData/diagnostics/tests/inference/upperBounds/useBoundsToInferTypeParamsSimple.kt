// !WITH_NEW_INFERENCE
package a

fun <V: U, U> foo(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u
fun <U, V: U> bar(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u

fun test(a: Any, s: String) {
    val b = foo(a, s)
    checkItIsExactlyAny(a, arrayListOf(b))
    val c = bar(a, s)
    checkItIsExactlyAny(a, arrayListOf(c))
}

fun <T> checkItIsExactlyAny(<!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>l<!>: MutableList<T>) {}

fun <V : U, U> baz(<!UNUSED_PARAMETER!>v<!>: V, u: MutableSet<U>) = u

fun test(a: Any, s: MutableSet<String>) {
    <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>baz<!>(a, s)
}

//from standard library
fun <T> arrayListOf(vararg <!UNUSED_PARAMETER!>t<!>: T): MutableList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>