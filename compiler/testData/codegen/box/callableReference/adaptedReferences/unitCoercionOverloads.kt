// IGNORE_BACKEND: ANDROID

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
