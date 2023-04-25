// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

suspend fun foo() = baz<Unit>()
suspend fun bar() = baz<Any>()
suspend fun <T> baz(): T {
    TODO()
}
