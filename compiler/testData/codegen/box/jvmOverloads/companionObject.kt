// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
