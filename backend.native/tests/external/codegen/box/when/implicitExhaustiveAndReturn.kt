fun test(i: Int): String {
    when (i) {
        0 -> return "0"
        1 -> return "1"
    }
    return "OK"
}

fun box(): String = test(42)