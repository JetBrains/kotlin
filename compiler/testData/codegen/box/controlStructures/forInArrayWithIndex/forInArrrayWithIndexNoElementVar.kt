// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

val arr = arrayOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    for ((i, _) in arr.withIndex()) {
        s.append("$i;")
    }

    val ss = s.toString()
    return if (ss == "0;1;2;3;") "OK" else "fail: '$ss'"
}