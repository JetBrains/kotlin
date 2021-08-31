fun box(): String {
    val x1 = 1L
    if (x1 !in Long.MIN_VALUE..Long.MAX_VALUE)
        return "Failed"
    return "OK"
}
