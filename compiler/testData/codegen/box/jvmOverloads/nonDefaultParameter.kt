// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C {
    @kotlin.jvm.JvmOverloads public fun foo(o: String, k: String = "K"): String {
        return o + k
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", String::class.java)
    return m.invoke(c, "O") as String
}
