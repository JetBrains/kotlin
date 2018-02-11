// WITH_RUNTIME

const val M = 0.toChar()

fun box(): String {
    var count = 0
    for (i in (M downTo M).reversed()) {
        ++count
        if (count > 1) {
            throw AssertionError("Loop should be executed once")
        }
    }
    if (count != 1) throw AssertionError("Should be executed once")
    return "OK"
}