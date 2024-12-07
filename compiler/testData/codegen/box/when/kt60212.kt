fun box(): String {
    testIfs()
    testWhen()

    return "OK"
}

// From KT-60212
fun testIfs() {
    val b1 = 0b11011111
    if (b1 and 0b10000000 == 0) {
        fail("A")
    } else if (b1 and 0b11100000 == 0b11000000) {
        return
    } else {
        fail("C")
    }
}

// Snippet from kotlinx-io
fun testWhen() {
    val v = test(0xdf)
    if (v != 1) fail("D")
}

fun test(b0: Int) = when {
    b0 and 0x80 == 0 -> 0
    b0 and 0xe0 == 0xc0 -> 1
    b0 and 0xf0 == 0xe0 -> 2
    b0 and 0xf8 == 0xf0 -> 3
    else -> -1
}

fun fail(message: String): Nothing = throw Exception(message)
