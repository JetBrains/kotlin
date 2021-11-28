// Will be executed on JDK 9, 11, 17
fun test(s: String): String {
    return "\ud83c" + s + "\udf09";
}

fun box() : String {
    return if (test("") == "\ud83c\udf09") "OK" else "fail: ${test("")}"
}