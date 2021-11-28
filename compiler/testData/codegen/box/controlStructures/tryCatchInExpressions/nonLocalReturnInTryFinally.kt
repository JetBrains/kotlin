inline fun g(block: () -> Unit): Unit = block()

fun box(): String {
    try {
        for (c in emptyArray<String>()) {
            g {
                for (d in emptyArray<Int>()) {
                    return "Fail"
                }
            }
        }
    } finally {}
    return "OK"
}
