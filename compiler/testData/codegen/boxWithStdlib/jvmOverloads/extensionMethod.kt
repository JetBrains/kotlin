class C {
}

[kotlin.jvm.overloads] fun C.foo(o: String, k: String = "K"): String {
    return o + k
}

fun box(): String {
    val m = javaClass<C>().getClassLoader().loadClass("_DefaultPackage").getMethod("foo", javaClass<C>(), javaClass<String>())
    return m.invoke(null, C(), "O") as String
}
