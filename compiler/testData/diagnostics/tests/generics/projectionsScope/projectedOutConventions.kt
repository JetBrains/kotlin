// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a <!MEMBER_PROJECTED_OUT{OI}!>+<!> <!TYPE_MISMATCH{NI}!>""<!>
    <!MEMBER_PROJECTED_OUT{OI}!>a[1]<!> = <!TYPE_MISMATCH{NI}!>""<!>
    <!MEMBER_PROJECTED_OUT{OI}!>a[<!TYPE_MISMATCH{NI}!>""<!>]<!>
}
