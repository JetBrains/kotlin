// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val l = ArrayList<Boolean>()
    l.add(true)
    val x = !l[0]
    if (x) return "Fail: $x}"
    return "OK"
}