// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

open class MyClass {
    fun def(i: Int = 0): Int {
        return i
    }
}

fun box():String {
    val method = MyClass::class.java.getMethod("def\$default", MyClass::class.java, Int::class.java, Int::class.java, Any::class.java)
    val result = method.invoke(null, MyClass(), -1, 1, null)

    if (result != 0) return "fail 1: $result"

    var failed = false
    try {
        method.invoke(null, MyClass(), -1, 1, "fail")
    }
    catch(e: Exception) {
        val cause = e.cause
        if (cause is UnsupportedOperationException && cause.message!!.startsWith("Super calls")) {
            failed = true
        }
    }

    return if (!failed) "fail" else "OK"
}
