class A {
    public lateinit var str: String
}

fun box(): String {
    val a = A()
    try {
        a.str
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "FAIL"
}