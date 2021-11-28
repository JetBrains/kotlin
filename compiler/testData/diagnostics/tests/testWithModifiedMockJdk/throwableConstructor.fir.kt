// !JDK_KIND: MODIFIED_MOCK_JDK
abstract class A : Throwable(1.0) {}

fun foo() {
    Throwable(1.5)
}
