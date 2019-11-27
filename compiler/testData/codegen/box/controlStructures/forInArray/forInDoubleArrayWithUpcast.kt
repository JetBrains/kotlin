// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    testForInFloatArrayWithUpcastToAny()
    testForInDoubleArrayWithUpcastToAny()
    testForInDoubleArrayWithUpcastToComparable()

    return "OK"
}

// NB JS: 1.0.toString() = "1"
// Thus we compare resulting string both with "1.0;2.0;3.0;" and "1;2;3;".

private fun testForInFloatArrayWithUpcastToAny() {
    var test = ""
    for (x: Any in floatArrayOf(1.0f, 2.0f, 3.0f)) {
        test = "$test$x;"
        useFloatAsAny(x)
    }
    if (test != "1.0;2.0;3.0;" && test != "1;2;3;") throw AssertionError(test)
}

private fun testForInDoubleArrayWithUpcastToAny() {
    var test = ""
    for (x: Any in doubleArrayOf(1.0, 2.0, 3.0)) {
        test = "$test$x;"
        useDoubleAsAny(x)
    }
    if (test != "1.0;2.0;3.0;" && test != "1;2;3;") throw AssertionError(test)
}

private fun testForInDoubleArrayWithUpcastToComparable() {
    var test = ""
    for (x: Comparable<*> in doubleArrayOf(1.0, 2.0, 3.0)) {
        test = "$test$x;"
        useDoubleAsComparable(x)
    }
    if (test != "1.0;2.0;3.0;" && test != "1;2;3;") throw AssertionError(test)
}

private fun useFloatAsAny(a: Any) {
    a as Float
}

private fun useDoubleAsAny(a: Any) {
    a as Double
}

private fun useDoubleAsComparable(a: Comparable<*>) {
    a as Double
}