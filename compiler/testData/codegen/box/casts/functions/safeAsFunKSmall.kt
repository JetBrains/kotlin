// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_STDLIB

fun fn0() {}
fun fn1(x: Any) {}

inline fun safeAsReturnsNull(operation: String, cast: () -> Any?) {
    try {
        val x = cast()
        assert(x == null) { "$operation: should return null, got $x" }
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

inline fun safeAsReturnsNonNull(operation: String, cast: () -> Any?) {
    try {
        val x = cast()
        assert(x != null) { "$operation: should return non-null" }
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    safeAsReturnsNonNull("f0 as? Function0<*>") { f0 as? Function0<*> }
    safeAsReturnsNull("f0 as? Function1<*, *>") { f0 as? Function1<*, *> }
    safeAsReturnsNull("f1 as? Function0<*>") { f1 as? Function0<*> }
    safeAsReturnsNonNull("f1 as? Function1<*, *>") { f1 as? Function1<*, *> }

    return "OK"
}
