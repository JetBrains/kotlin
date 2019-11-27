// IGNORE_BACKEND_FIR: JVM_IR
fun test1(): Long {
    var s = 0L
    for (i in 1L..4) {
        s = s * 10 + i
    }
    return s
}

fun test2(): Long {
    var s = 0L
    for (i in 1L..4.toShort()) {
        s = s * 10 + i
    }
    return s
}

fun testLI(a: Long, b: Int): Long {
    var s = 0L
    for (i in a..b) {
        s = s * 10 + i
    }
    return s
}

fun testLS(a: Long, b: Short): Long {
    var s = 0L
    for (i in a..b) {
        s = s * 10 + i
    }
    return s
}

fun box(): String {
    if (test1() != 1234L) return "Fail 1"
    if (test2() != 1234L) return "Fail 1"
    if (testLI(1L, 4) != 1234L) return "Fail 2"
    if (testLS(1L, 4.toShort()) != 1234L) return "Fail 2"
    return "OK"
}