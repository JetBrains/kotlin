// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val arr = Array(4) { arrayOf("x$it") }

    var s = ""
    for ((i, sarr) in arr.withIndex()) {
        s += "$i:${sarr.toList()}"
    }

    return if (s != "0:[x0]1:[x1]2:[x2]3:[x3]") "Fail: '$s'" else "OK"
}