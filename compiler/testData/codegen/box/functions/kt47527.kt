// ISSUE: KT-47527
// WITH_STDLIB

fun test_1(value: Any?): String? = value?.let { return "O" }
fun test_2(value: Any?): String? = run {
    value?.let { return "K" }
}

fun box(): String {
    var result = ""
    result += test_1(1) ?: return "fail 1"
    result += test_2(1) ?: return "fail 2"
    return result
}
