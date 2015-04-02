class C {
    [kotlin.jvm.overloads] public fun foo(o: String, k: String = "K"): String {
        return o + k
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", javaClass<String>())
    return m.invoke(c, "O") as String
}
