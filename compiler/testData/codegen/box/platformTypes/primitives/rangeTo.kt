// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Int>()
    l.add(2)
    val sb = StringBuilder()
    for (i in l[0]..3) {
        sb.append(i)
    }
    if (sb.toString() != "23") return "Fail: $sb}"
    return "OK"
}