// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

const val M = Int.MAX_VALUE

fun box(): String {
    var step = 0
    for (i in (M .. M).reversed()) {
        ++step
        if (step > 1) throw AssertionError("Should be executed once")
    }
    if (step != 1) throw AssertionError("Should be executed once")

    return "OK"
}