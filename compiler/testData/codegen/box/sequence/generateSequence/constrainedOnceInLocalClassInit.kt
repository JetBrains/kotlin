// WITH_STDLIB
fun box(): String {
    var calls = 0
    val s = generateSequence { if (calls++ < 2) calls else null }
    val objects = mutableListOf<Any>()
    class L {
        init {
            for (x in s) objects += x
        }
    }
    L()
    try {
        L()
    } catch (e: IllegalStateException) {
        return "OK"
    }
    return "Fail: no exception"
}
