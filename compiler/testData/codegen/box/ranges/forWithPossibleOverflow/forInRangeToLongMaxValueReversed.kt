// WITH_RUNTIME

const val M = Long.MAX_VALUE

fun box(): String {
    var count = 0
    for (i in (M .. M).reversed()) {
        ++count
        if (count > 1) {
            throw AssertionError("Loop should be executed once")
        }
    }
    if (count != 1) throw AssertionError("Should be executed once")
    return "OK"
}