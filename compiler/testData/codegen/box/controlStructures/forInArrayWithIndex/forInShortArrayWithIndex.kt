// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = shortArrayOf(10, 20, 30, 40)

fun box(): String {
    val s = StringBuilder()
    for ((index, x) in arr.withIndex()) {
        s.append("$index:$x;")
    }
    val ss = s.toString()
    return if (ss == "0:10;1:20;2:30;3:40;") "OK" else "fail: '$ss'"
}