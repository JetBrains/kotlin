// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C {
    companion object {
        @JvmStatic @kotlin.jvm.JvmOverloads public fun foo(o: String, k: String = "K"): String {
            return o + k
        }
    }
}

fun box(): String {
    val m = C::class.java.getMethod("foo", String::class.java)
    return m.invoke(null, "O") as String
}
