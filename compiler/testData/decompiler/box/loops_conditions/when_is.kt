fun bar() = 666

fun box(): String {
    val f = bar()
    when (f) {
        !is Int -> return "FAIL"
        is Int -> return "OK"
        else -> return "FAIL"
    }
}