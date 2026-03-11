// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: result.getMethod OK in FE1.0, unresolved in FIR
// FULL_JDK

val defaultStringConverter = fun(s: String): Any {
    var result: Any = s
    var m: Array<String>? = arrayOf("1", "2", "3", "4")
    if (m != null) {
        val fname = m[4]
        try {
            result = Class.forName(m[1])
            if (fname != "") {
                try {
                    val f = result.getField(fname)
                    result = f.get(null)
                } catch (nfe: NoSuchFieldException) {
                    val meth = result.getMethod(fname)
                }
            }
        } catch (cnfe: ClassNotFoundException) {
        }
    }
    return result
}

// Just check that there's no VerifyError.
// Semantics is checked in kt47851.kt.
fun box() = "OK"
