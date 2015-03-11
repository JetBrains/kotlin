open class A<T> {
    val prop: String
    constructor(x: String) {
        prop = x
    }
    constructor(x: T) {
        prop = x.toString()
    }

    override fun toString() = prop
}

fun box(): String {
    val a1 = WithGenerics.foo1()
    if (a1 != "OK") return "fail1: $a1"
    val a2 = WithGenerics.foo2()
    if (a2 != "123") return "fail2: $a2"

    return "OK"
}
