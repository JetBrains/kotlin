// ORIGINAL: /compiler/testData/diagnostics/tests/testWithModifiedMockJdk/throwableConstructor.fir.kt
// WITH_STDLIB
// !JDK_KIND: MODIFIED_MOCK_JDK
abstract class A : Throwable(1.0) {}

fun foo() {
    Throwable(1.5)
}


fun box() = "OK".also { foo() }
