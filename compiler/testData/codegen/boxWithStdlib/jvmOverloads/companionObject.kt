class C {
    companion object {
        [kotlin.platform.platformStatic] [kotlin.jvm.overloads] public fun foo(o: String, k: String = "K"): String {
            return o + k
        }
    }
}

fun box(): String {
    val m = javaClass<C>().getMethod("foo", javaClass<String>())
    return m.invoke(null, "O") as String
}
