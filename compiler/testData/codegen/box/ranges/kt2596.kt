fun box(): String {
    val iterator: Iterator<Int> = (0..0).iterator()
    for (i in iterator) {
        return "OK"
    }
    return "fail"
}