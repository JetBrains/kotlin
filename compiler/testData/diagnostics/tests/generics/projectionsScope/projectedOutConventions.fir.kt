// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a + <!MEMBER_PROJECTED_OUT!>""<!>
    a[1] = <!MEMBER_PROJECTED_OUT!>""<!>
    a[<!MEMBER_PROJECTED_OUT!>""<!>]
}
