// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(x: T, y: T) {}
}

fun test(a: A<out CharSequence>) {
    a.<!MEMBER_PROJECTED_OUT!>foo<!>("", "")
}
