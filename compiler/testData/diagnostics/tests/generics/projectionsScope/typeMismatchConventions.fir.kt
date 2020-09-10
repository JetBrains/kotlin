// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: Out<T>): A<T> = this
    operator fun set(x: Int, y: Out<T>) {}
    operator fun get(x: Out<T>) = 1
}

class Out<out F>

fun test(a: A<out CharSequence>, y: Out<CharSequence>) {
    a <!INAPPLICABLE_CANDIDATE!>+<!> y
    <!INAPPLICABLE_CANDIDATE!>a[1] = y<!>
    <!INAPPLICABLE_CANDIDATE!>a[y]<!>

    a + Out<Nothing>()
    a[1] = Out<Nothing>()
    a[Out<Nothing>()]
}
