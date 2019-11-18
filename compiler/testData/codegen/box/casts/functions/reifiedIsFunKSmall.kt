// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

fun fn0() {}
fun fn1(x: Any) {}

inline fun <reified T> assertReifiedIs(x: Any, type: String) {
    val answer: Boolean
    try {
        answer = x is T
    }
    catch (e: Throwable) {
        throw AssertionError("$x is $type: should not throw exceptions, got $e")
    }
    assert(answer) { "$x is $type: failed" }
}

inline fun <reified T> assertReifiedIsNot(x: Any, type: String) {
    val answer: Boolean
    try {
        answer = x !is T
    }
    catch (e: Throwable) {
        throw AssertionError("$x !is $type: should not throw exceptions, got $e")
    }
    assert(answer) { "$x !is $type: failed" }
}

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    assertReifiedIs<Function0<*>>(f0, "Function0<*>")
    assertReifiedIs<Function1<*, *>>(f1, "Function1<*, *>")
    assertReifiedIsNot<Function0<*>>(f1, "Function0<*>")
    assertReifiedIsNot<Function1<*, *>>(f0, "Function1<*, *>")

    return "OK"
}
