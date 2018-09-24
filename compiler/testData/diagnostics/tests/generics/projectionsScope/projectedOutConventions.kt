// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a <!OI;MEMBER_PROJECTED_OUT!>+<!> <!NI;TYPE_MISMATCH!>""<!>
    <!OI;MEMBER_PROJECTED_OUT!>a[1]<!> = <!NI;TYPE_MISMATCH!>""<!>
    <!OI;MEMBER_PROJECTED_OUT!>a[<!NI;TYPE_MISMATCH!>""<!>]<!>
}
