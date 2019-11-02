fun fn0() {
}

fun fn1(x: Any) {
}

inline fun <T> reifiedSafeAsReturnsNonNull(x: Any?, operation: String) {
    val y : T? = try {
    (x as? T)}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should not throw exceptions, got ${e}")
}
    if (y == null) {
        throw AssertionError("${operation}: should return non-null, got null")
    }
}

inline fun <T> reifiedSafeAsReturnsNull(x: Any?, operation: String) {
    val y : T? = try {
    (x as? T)}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should not throw exceptions, got ${e}")
}
    if (y != null) {
        throw AssertionError("${operation}: should return null, got ${y}")
    }
}

fun box() : String  {
    val f0 : Any = (::fn0 as Any)
    val f1 : Any = (::fn1 as Any)
    reifiedSafeAsReturnsNonNull<() -> Any?>(f0, "f0 as Function0<*>")
    reifiedSafeAsReturnsNull<(Any?) -> Any?>(f0, "f0 as Function1<*, *>")
    reifiedSafeAsReturnsNull<() -> Any?>(f1, "f1 as Function0<*>")
    reifiedSafeAsReturnsNonNull<(Any?) -> Any?>(f1, "f1 as Function1<*, *>")
    reifiedSafeAsReturnsNull<() -> Any?>(null, "null as Function0<*>")
    reifiedSafeAsReturnsNull<(Any?) -> Any?>(null, "null as Function1<*, *>")
    return "OK"
}
