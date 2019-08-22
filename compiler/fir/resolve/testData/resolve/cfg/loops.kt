fun testWhile(b: Boolean, x: Any?) {
    while (b) {
        val y = x is String
    }
    x is String
}

fun testDoWhile(b: Boolean, x: Any?) {
    do {
        val y = x is String
    } while (b)
    x is String
}

fun testFor(x: Any?) {
    for (i in 0..5) {
        val y = x is String
    }
    x is String
}