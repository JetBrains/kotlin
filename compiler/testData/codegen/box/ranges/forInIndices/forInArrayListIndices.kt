// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun box(): String {
    val a = ArrayList<String>()
    a.add("OK")
    for (i in a.indices) {
        return a[i]
    }
    return "Fail"
}
