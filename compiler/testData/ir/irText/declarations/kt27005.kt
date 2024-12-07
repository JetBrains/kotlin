// FIR_IDENTICAL

suspend fun foo() = baz<Unit>()
suspend fun bar() = baz<Any>()
suspend fun <T> baz(): T {
    TODO()
}
