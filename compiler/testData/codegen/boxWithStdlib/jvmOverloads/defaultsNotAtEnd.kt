class C {
    [kotlin.jvm.overloads] public fun foo(o: String = "O", i1: Int, k: String = "K", i2: Int): String {
        return o + k
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", javaClass<Int>(), javaClass<Int>())
    return m.invoke(c, 1, 2) as String
}
