// TARGET_BACKEND: JS_IR
// ISSUE: KT-57682
fun test_1(x: String) = x.length

fun test_2(x: dynamic): Int = when (x) {
    is String -> x.length
    else -> x.something
}

fun box(): String {
    val result = test_1("a") + test_2("ab")
    return if (result == 3) "OK" else "fail"
}
