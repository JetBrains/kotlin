// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class A<out T, in E> {
    fun foo(x: @UnsafeVariance T) {}
    fun foo(x: @UnsafeVariance T, y: List<@UnsafeVariance T>): @UnsafeVariance E = null!!

    fun bar(): List<@UnsafeVariance E> = null!!
}

fun foo(x: A<String, Any?>, cs: CharSequence, ls: List<CharSequence>) {
    val y: A<CharSequence, String> = x

    y.<!INAPPLICABLE_CANDIDATE!>foo<!>(cs)
    // behaviour is different from old front-end
    // the type of y.foo() was String, but now it is Any?
    val s: String = <!INITIALIZER_TYPE_MISMATCH!>y.<!INAPPLICABLE_CANDIDATE!>foo<!>(cs, ls)<!>

    // behaviour is different from old front-end
    // the type of y.foo() was List<String>, but now it is Any?
    val ls2: List<String> = <!INITIALIZER_TYPE_MISMATCH!>y.bar()<!>
}
