
// WITH_STDLIB

fun testBreak() {
    for (i in 0..1) {
        for (j in break downTo 1u) {}
    }
}

fun testReturn() {
    for (i in 0..1) {
        for (j in (return) downTo 1u) {}
    }
}

fun testThrow() {
    try {
        for (i in 0..1) {
            for (j in (throw Exception()) downTo 1u) {
            }
        }
    } catch (e: Exception) {}
}

fun box(): String {
    testBreak()
    testReturn()
    testThrow()
    return "OK"
}