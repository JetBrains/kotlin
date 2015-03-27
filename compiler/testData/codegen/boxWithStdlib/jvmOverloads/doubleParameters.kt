class C {
    [kotlin.jvm.overloads] public fun foo(d1: Double, d2: Double, status: String = "OK"): String {
        return if (d1 + d2 == 3.0) status else "fail"
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo", javaClass<Double>(), javaClass<Double>())
    return m.invoke(c, 1.0, 2.0) as String
}
