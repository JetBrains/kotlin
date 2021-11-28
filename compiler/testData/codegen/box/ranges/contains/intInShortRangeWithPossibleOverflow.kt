fun box(): String {
    val x1 = 1
    if (x1 !in Short.MIN_VALUE..Short.MAX_VALUE)
        return "Failed"
    return "OK"
}
