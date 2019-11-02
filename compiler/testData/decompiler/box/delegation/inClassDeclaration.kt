interface Base {
    fun getValue(): String
    fun test(): String = getValue()
}

class Fail : Base {
    override fun getValue() = "Fail"
}

class Derived : Base by Fail() {
    override fun getValue() = "OK"
}

fun box(): String {
    return Derived().test()
}
