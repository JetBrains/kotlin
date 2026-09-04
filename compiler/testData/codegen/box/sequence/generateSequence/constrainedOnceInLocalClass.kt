// WITH_STDLIB
fun box(): String {
    var calls = 0
    val s = generateSequence { if (calls++ < 2) calls else null }
    class L {
        val x = s.toList()
    }
    L()
    try {
        L()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "Fail: no exception"
}
