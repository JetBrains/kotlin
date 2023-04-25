// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

fun test1() {
    throw Throwable()
}

fun testImplicitCast(a: Any) {
    if (a is Throwable) {
        throw a
    }
}
