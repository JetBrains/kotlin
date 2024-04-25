// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(x: T, y: T) {}
}

fun test(a: A<out CharSequence>) {
    a.foo(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
}
