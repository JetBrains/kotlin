// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val a = ArrayList<Int>()
    a.add(74)
    a.add(75)
    val i: Int = a.get(0)
    val j: Int = a.get(1)
    if (i != 74) return "fail 1"
    if (j != 75) return "fail 2"
    if (a.size != 2) return "epic fail"
    return "OK"
}
