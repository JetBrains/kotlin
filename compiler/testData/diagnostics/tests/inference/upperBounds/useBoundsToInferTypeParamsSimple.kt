package a

fun foo<V: U, U>(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u
fun bar<U, V: U>(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u

fun test(a: Any, s: String) {
    val b = foo(a, s)
    checkItIsExactlyAny(a, arrayListOf(b))
    val c = bar(a, s)
    checkItIsExactlyAny(a, arrayListOf(c))
}

fun checkItIsExactlyAny<T>(<!UNUSED_PARAMETER!>t<!>: T, <!UNUSED_PARAMETER!>l<!>: MutableList<T>) {}

fun baz<V : U, U>(<!UNUSED_PARAMETER!>v<!>: V, u: MutableSet<U>) = u

fun test(a: Any, s: MutableSet<String>) {
    <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>baz<!>(a, s)
}

//from standard library
fun arrayListOf<T>(vararg <!UNUSED_PARAMETER!>t<!>: T): MutableList<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>