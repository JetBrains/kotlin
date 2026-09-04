// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
fun throwingElement(z: Int): Int {
    var s = 0
    for (x in sequenceOf(
        1,
        2 / z,
    )) s += x
    return s
}

fun box(): String {
    val e = try { throwingElement(0); null } catch (e: ArithmeticException) { e }
        ?: return "Fail: no exception"
    val frame = e.stackTrace.first { it.methodName == "throwingElement" }
    // the line of `2 / z`
    return if (frame.lineNumber == 8) "OK" else "Fail: line ${frame.lineNumber}"
}

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt
