fun box(): String {
    val c1: Char = 0.toChar()
    val c2 = c1 - 1
    if (c2 < c1) return "fail: 0.toChar() - 1 should overflow to positive."

    val c3 = c2 + 1
    if (c3 > c2) return "fail: FFFF.toChar() + 1 should overflow to zero."

    return "OK"
}