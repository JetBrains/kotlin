// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: T): A<T> = this
    operator fun set(x: Int, y: T) {}
    operator fun get(x: T) = 1
}

fun test(a: A<out CharSequence>) {
    a <!INAPPLICABLE_CANDIDATE!>+<!> ""
    <!INAPPLICABLE_CANDIDATE!>a[1] = ""<!>
    <!INAPPLICABLE_CANDIDATE!>a[""]<!>
}
