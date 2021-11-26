// WITH_STDLIB

fun <T: CharSequence> test(s: T): Int {
    var result = 0
    for (i in s.indices) {
        result = result * 10 + (i + 1)
    }
    return result
}

fun box(): String {
    val test = test("abcd")
    if (test != 1234) return "Fail: $test"

    return "OK"
}