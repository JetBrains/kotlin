fun test(i: Int): Int {
    return i
}

fun box(): String {
    var aByte: Byte? = 0
    var bByte: Byte = 0

    if (aByte != null) {
        if (aByte.dec() != bByte.dec()) return "Failed post-decrement Byte: ${aByte.dec()} != ${bByte.dec()}"
    }

    if (aByte != null) {
        if (aByte.inc() != bByte.inc()) return "Failed post-increment Byte: ${aByte.inc()} != ${bByte.inc()}"
    }

    aByte = null

    return "OK"
}