fun box(): String {
    var s = ""
    try {
        throw RuntimeException()
    } catch (e : RuntimeException) {
    } finally {
        s += "OK"
    }
    return s
}
