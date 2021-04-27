// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class A<out T, in E> {
    fun foo(x: @UnsafeVariance T) {}
    fun foo(x: @UnsafeVariance T, y: List<@UnsafeVariance T>): @UnsafeVariance E = null!!

    fun bar(): List<@UnsafeVariance E> = null!!
}

fun foo(x: A<String, Any?>, cs: CharSequence, ls: List<CharSequence>) {
    val y: A<CharSequence, String> = x

    y.foo(<!ARGUMENT_TYPE_MISMATCH!>cs<!>)
    val s: String = <!INITIALIZER_TYPE_MISMATCH!>y.foo(<!ARGUMENT_TYPE_MISMATCH!>cs<!>, <!ARGUMENT_TYPE_MISMATCH!>ls<!>)<!>

    val ls2: List<String> = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>y.bar()<!>
}
