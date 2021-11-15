// WITH_STDLIB

fun testContinue() {
    for (i in 0..1) {
        for (j in continue downTo 1u) {}
    }
}

fun box(): String {
    testContinue()
    return "OK"
}