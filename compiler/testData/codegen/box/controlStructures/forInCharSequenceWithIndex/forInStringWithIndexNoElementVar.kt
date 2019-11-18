// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

val xs = "abcd"

fun box(): String {
    val s = StringBuilder()

    for ((i, _) in xs.withIndex()) {
        s.append("$i;")
    }

    val ss = s.toString()
    return if (ss == "0;1;2;3;") "OK" else "fail: '$ss'"
}