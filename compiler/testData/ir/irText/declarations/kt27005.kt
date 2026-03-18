// FIR_IDENTICAL
// IGNORE_BACKEND: JKLIB

suspend fun foo() = baz<Unit>()
suspend fun bar() = baz<Any>()
suspend fun <T> baz(): T {
    TODO()
}
