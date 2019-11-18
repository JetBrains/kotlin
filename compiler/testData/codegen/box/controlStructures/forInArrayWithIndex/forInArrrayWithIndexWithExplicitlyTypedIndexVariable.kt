// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = arrayOf("a", "b", "c", "d")

fun useAny(x: Any) {}

fun box(): String {
    val s = StringBuilder()

    for ((index: Any, x) in arr.withIndex()) {
        useAny(index)
        s.append("$index:$x;")
    }

    val ss = s.toString()
    return if (ss == "0:a;1:b;2:c;3:d;") "OK" else "fail: '$ss'"
}