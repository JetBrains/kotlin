// IGNORE_BACKEND_K1: ANY
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 creates non-private backing field which does not pass IR Validation in compiler v2.2.0

lateinit var result: String

fun foo(x: Int, y: Any): Int {
    result = "OK"
    return x
}
fun foo(x: Any, y: Int): Int {
    result = "FAIL"
    return y
}

fun box(): String {
    val fooRef: (Int, Any) -> Unit = ::foo
    fooRef(1, "")
    return result
}
