// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo(x: T, y: T) {}
}

fun test(a: A<out CharSequence>) {
    a.foo(<!MEMBER_PROJECTED_OUT!>""<!>, <!MEMBER_PROJECTED_OUT!>""<!>)
}
