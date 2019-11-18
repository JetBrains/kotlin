// IGNORE_BACKEND_FIR: JVM_IR
fun f(x: Any?): Any? {
    if (x is Array<*>) {
        for (i in x) {
            return i
        }
    }
    return "FAIL"
}

fun box(): String {
    val a = arrayOfNulls<String>(1) as Array<String>
    a[0] = "OK"
    return f(a) as String
}
