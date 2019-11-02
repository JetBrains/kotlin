fun fn0() {
}

fun fn1(x: Any) {
}

inline fun <T> reifiedAsSucceeds(x: Any, operation: String) {
    try {
    (x as T)}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should not throw exceptions, got ${e}")
}
}

inline fun <T> reifiedAsFailsWithCCE(x: Any, operation: String) {
    try {
    (x as T)}
catch (e : ClassCastException)  {
    return Unit
}
catch (e : Throwable)  {
    throw AssertionError("${operation}: should throw ClassCastException, got ${e}")
}
    throw AssertionError("${operation}: should fail with CCE, no exception thrown")
}

fun box() : String  {
    val f0 : Any = (::fn0 as Any)
    val f1 : Any = (::fn1 as Any)
    reifiedAsSucceeds<() -> Any?>(f0, "f0 as Function0<*>")
    reifiedAsFailsWithCCE<(Any?) -> Any?>(f0, "f0 as Function1<*, *>")
    reifiedAsFailsWithCCE<() -> Any?>(f1, "f1 as Function0<*>")
    reifiedAsSucceeds<(Any?) -> Any?>(f1, "f1 as Function1<*, *>")
    return "OK"
}
