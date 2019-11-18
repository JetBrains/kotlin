// !LANGUAGE: +AllowBreakAndContinueInsideWhen
// IGNORE_BACKEND_FIR: JVM_IR

fun testFor() {
    val xs = IntArray(10) { i -> i }
    var k = 0
    for (x in xs) {
        when {
            k > 2 -> break
        }
        ++k
    }
    if (k != 3) throw AssertionError()
}

fun testWhile() {
    var k = 0
    while (k < 10) {
        when {
            k > 2 -> break
        }
        ++k
    }
    if (k != 3) throw AssertionError()
}

fun testDoWhile() {
    var k = 0
    do {
        when {
            k > 2 -> break
        }
        ++k
    } while (k < 10)
    if (k != 3) throw AssertionError()
}

fun box(): String {
    testFor()
    testWhile()
    testDoWhile()

    return "OK"
}