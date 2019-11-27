// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = arrayOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    for ((_, x) in arr.withIndex()) {
        s.append("$x;")
    }

    val ss = s.toString()
    return if (ss == "a;b;c;d;") "OK" else "fail: '$ss'"
}