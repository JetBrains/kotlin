// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_RUNTIME

val xs = arrayListOf("a", "b", "c", "d")

fun box(): String {
    val s = StringBuilder()

    var cmeThrown = false
    try {
        for ((index, x) in xs.withIndex()) {
            s.append("$index:$x;")
            xs.clear()
        }
    } catch (e: java.util.ConcurrentModificationException) {
        cmeThrown = true
    }

    if (!cmeThrown) return "Fail: CME should be thrown"

    val ss = s.toString()
    return if (ss == "0:a;") "OK" else "fail: '$ss'"
}