// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a + <!TYPE_MISMATCH!>""<!>
    a[1] = <!TYPE_MISMATCH!>""<!>
    a[<!TYPE_MISMATCH!>""<!>]
}
