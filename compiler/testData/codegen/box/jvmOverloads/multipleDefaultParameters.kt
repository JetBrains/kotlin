// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C {
    @kotlin.jvm.JvmOverloads public fun foo(o: String = "O", k: String = "K"): String {
        return o + k
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo")
    return m.invoke(c) as String
}
