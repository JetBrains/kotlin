// !LANGUAGE: +AllowBreakAndContinueInsideWhen
// IGNORE_BACKEND: JS_IR

// KT-61141: throws kotlin.AssertionError instead of java.lang.AssertionError
// IGNORE_BACKEND: NATIVE

fun testBreakFor() {
    val xs = IntArray(10) { i -> i }
    var k = 0
    for (x in xs) {
        when {
            k > 2 -> break
        }
    }
}

fun testBreakWhile() {
    var k = 0
    while (k < 10) {
        when {
            k > 2 -> break
        }
    }
}

fun testBreakDoWhile() {
    var k = 0
    do {
        when {
            k > 2 -> break
        }
    } while (k < 10)
}

fun testContinueFor() {
    val xs = IntArray(10) { i -> i }
    var k = 0
    for (x in xs) {
        when {
            k > 2 -> continue
        }
    }
}

fun testContinueWhile() {
    var k = 0
    while (k < 10) {
        when {
            k > 2 -> continue
        }
    }
}

fun testContinueDoWhile() {
    var k = 0
    var s = ""
    do {
        ++k
        when {
            k > 2 -> continue
        }
        s += "$k;"
    } while (k < 10)
    if (s != "1;2;") throw AssertionError(s)
}
