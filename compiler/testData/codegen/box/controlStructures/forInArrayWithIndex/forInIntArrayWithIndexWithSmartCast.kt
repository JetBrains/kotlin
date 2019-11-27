// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val arr = intArrayOf(10, 20, 30, 40)

fun foo(xs: Any): String {
    if (xs !is IntArray) return "not an IntArray"

    val s = StringBuilder()
    for ((index, x) in xs.withIndex()) {
        s.append("$index:$x;")
    }
    return s.toString()
}

fun box(): String {
    val ss = foo(arr)
    return if (ss == "0:10;1:20;2:30;3:40;") "OK" else "fail: '$ss'"
}