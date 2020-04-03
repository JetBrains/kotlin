// !DUMP_CFG
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

fun testWhileTrue() {
    while (true) {
        1
    }
    1
}

fun testWhileTrueWithBreak(b: Boolean) {
    while (true) {
        if (b) {
            break
        }
    }
    1
}


fun testWhileFalse() {
    while (false) {
        1
    }
    1
}

fun testDoWhileTrue() {
    do {
        1
    } while (true)
    1
}

fun testDoWhileTrueWithBreak(b: Boolean) {
    do {
        if (b) {
            break
        }
    } while (true)
    1
}

fun testDoWhileFalse() {
    do {
        1
    } while (false)
    1
}