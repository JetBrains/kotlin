interface A<T> {
    open fun foo(t: T) = "A"
}

interface B : A<String>

enum class Z(val name: String) : B {
    Z1("Z1"),
    Z2("Z2");
    override fun foo(t: String) = name
}


fun box(): String {
    return when {
        Z.Z1.foo("")               != "Z1" -> "Fail #1"
        Z.Z2.foo("")               != "Z2" -> "Fail #2"
        (Z.Z1 : B).foo("")         != "Z1" -> "Fail #3"
        (Z.Z2 : B).foo("")         != "Z2" -> "Fail #4"
        (Z.Z1 : A<String>).foo("") != "Z1" -> "Fail #5"
        (Z.Z2 : A<String>).foo("") != "Z2" -> "Fail #6"
        else -> "OK"
    }
}
