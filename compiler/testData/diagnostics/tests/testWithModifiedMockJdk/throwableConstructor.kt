// !JDK_KIND: MODIFIED_MOCK_JDK
abstract class A : <!DEPRECATION!>Throwable<!>(1.0) {}

fun foo() {
    <!DEPRECATION!>Throwable<!>(1.5)
}
