// TARGET_BACKEND: JVM

// WITH_RUNTIME

class C {
    @kotlin.jvm.JvmOverloads public fun foo(o: String = "O", i1: Int, k: String = "K", i2: Int): String {
        return o + k
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", Int::class.java, Int::class.java)
    return m.invoke(c, 1, 2) as String
}
