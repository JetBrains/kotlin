const val M = 0xFFFF.toChar()

fun box(): String {
    var count = 0
    for (i in M .. M) {
        ++count
        if (count > 1) {
            throw AssertionError("Loop should be executed once")
        }
    }
    if (count != 1) throw AssertionError("Should be executed once")
    return "OK"
}