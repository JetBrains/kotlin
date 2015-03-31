class C [kotlin.jvm.overloads] (s1: String, s2: String = "K") {
    public val status: String = s1 + s2
}

fun box(): String {
    val c = (javaClass<C>().getConstructor(javaClass<String>()).newInstance("O"))
    return c.status
}
