// WITH_STDLIB

fun test(text: String): String {
    when (text.takeWhile { it.isLetter() }) {
        in arrayOf("a") -> return "OK"
    }
    return "FAIL"
}

fun box(): String = test("a")
