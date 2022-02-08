fun box(): String {
    val x1 = 1.toChar()
    if (x1 !in Char.MIN_VALUE..Char.MAX_VALUE)
        return "Failed"
    return "OK"
}
