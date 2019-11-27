// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val a = CharArray(5)
    val x = a.indices.iterator()
    while (x.hasNext()) {
        val i = x.next()
        if (a[i] != 0.toChar()) return "Fail $i ${a[i]}"
    }
    return "OK"
}
