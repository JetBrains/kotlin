// TARGET_BACKEND: JVM

// WITH_STDLIB

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
