package a

fun foo<V : U, U>(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u
fun bar<U, V : U>(<!UNUSED_PARAMETER!>v<!>: V, u: U) = u

fun test(a: Any, s: String) {
    val <!UNUSED_VARIABLE!>b<!>: Any = foo(a, s)
    val <!UNUSED_VARIABLE!>c<!>: Any = bar(a, s)
}

fun baz<V : U, U>(<!UNUSED_PARAMETER!>v<!>: V, u: MutableSet<U>) = u

fun test(a: Any, s: MutableSet<String>) {
    <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>baz<!>(a, s)
}
