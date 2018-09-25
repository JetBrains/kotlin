// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

const val M = Int.MIN_VALUE

fun box(): String {
    var count = 0
    for (i in (M until M + 1).reversed()) {
        ++count
        if (count > 1) {
            throw AssertionError("Loop should be executed once")
        }
    }
    if (count != 1) throw AssertionError("Should be executed once")
    return "OK"
}