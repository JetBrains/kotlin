// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C {
    @kotlin.jvm.JvmOverloads public fun foo(s: String = "OK"): String {
        return s
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo")
    return m.invoke(c) as String
}
