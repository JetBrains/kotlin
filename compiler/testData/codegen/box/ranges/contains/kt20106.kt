// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

fun box(): String {
    val strSet = setOf("a", "b")
    val xx = "a" to ("a" in strSet)
    return if (!xx.second) "fail" else "OK"
}