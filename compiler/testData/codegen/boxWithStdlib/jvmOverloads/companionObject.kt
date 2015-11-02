class C {
    companion object {
        @JvmStatic @kotlin.jvm.JvmOverloads public fun foo(o: String, k: String = "K"): String {
            return o + k
        }
    }
}

fun box(): String {
    val m = javaClass<C>().getMethod("foo", javaClass<String>())
    return m.invoke(null, "O") as String
}
