open class OtherClass {
    fun foo(): String = "OK"

    private class OtherClass<T> {}
}


class Derived : OtherClass()

fun box() = Derived().foo()

