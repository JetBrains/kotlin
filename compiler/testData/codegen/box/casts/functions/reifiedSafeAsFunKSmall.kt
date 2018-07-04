// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun fn0() {}
fun fn1(x: Any) {}

inline fun <reified T> reifiedSafeAsReturnsNonNull(x: Any?, operation: String) {
    val y = try {
        x as? T
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
    if (y == null) {
        throw AssertionError("$operation: should return non-null, got null")
    }
}

inline fun <reified T> reifiedSafeAsReturnsNull(x: Any?, operation: String) {
    val y = try {
        x as? T
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
    if (y != null) {
        throw AssertionError("$operation: should return null, got $y")
    }
}

fun box(): String {
    val f0 = ::fn0 as Any
    val f1 = ::fn1 as Any

    reifiedSafeAsReturnsNonNull<Function0<*>>(f0, "f0 as Function0<*>")
    reifiedSafeAsReturnsNull<Function1<*, *>>(f0, "f0 as Function1<*, *>")
    reifiedSafeAsReturnsNull<Function0<*>>(f1, "f1 as Function0<*>")
    reifiedSafeAsReturnsNonNull<Function1<*, *>>(f1, "f1 as Function1<*, *>")

    reifiedSafeAsReturnsNull<Function0<*>>(null, "null as Function0<*>")
    reifiedSafeAsReturnsNull<Function1<*, *>>(null, "null as Function1<*, *>")

    return "OK"
}