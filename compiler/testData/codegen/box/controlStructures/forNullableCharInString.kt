// WITH_STDLIB

fun box(): String {
    val str = "abcd"
    var r = ""
    for (c: Char? in str) {
        r = r + c ?: "?"
    }
    if (r != "abcd") throw AssertionError()

    return "OK"
}