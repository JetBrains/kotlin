// !LANGUAGE: +AllowBreakAndContinueInsideWhen
// IGNORE_BACKEND_FIR: JVM_IR

fun testFor() {
    val xs = IntArray(10) { i -> i }
    var k = 0
    var s = ""
    for (x in xs) {
        ++k
        when {
            k > 2 -> continue
        }
        s += "$k;"
    }
    if (s != "1;2;") throw AssertionError(s)
}

fun testWhile() {
    var k = 0
    var s = ""
    while (k < 10) {
        ++k
        when {
            k > 2 -> continue
        }
        s += "$k;"
    }
    if (s != "1;2;") throw AssertionError(s)
}

fun testDoWhile() {
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

fun box(): String {
    testFor()
    testWhile()
    testDoWhile()

    return "OK"
}