// DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    operator fun plus(x: Out<T>): A<T> = this
    operator fun set(x: Int, y: Out<T>) {}
    operator fun get(x: Out<T>) = 1
}

class Out<out F>

fun test(a: A<out CharSequence>, y: Out<CharSequence>) {
    a + <!ARGUMENT_TYPE_MISMATCH!>y<!>
    a[1] = <!ARGUMENT_TYPE_MISMATCH!>y<!>
    a[<!ARGUMENT_TYPE_MISMATCH!>y<!>]

    a + Out<Nothing>()
    a[1] = Out<Nothing>()
    a[Out<Nothing>()]
}
