class A {
    private lateinit val str: String

    init {
        str = "OK"
    }

    public fun getMyStr(): String {
        return str
    }
}

fun box(): String {
    val a = A()
    return a.getMyStr()
}