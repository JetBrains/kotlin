class A {
    public lateinit val str: String

    init {
        str = "OK"
    }
}

fun box(): String {
    val a = A()
    return a.str
}