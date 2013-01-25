open class A<T> {
    open fun foo(t: T) = "A"
}

open class B<T> : A<T>()

open class C : B<String>() {
    override fun foo(t: String) = "C"
}

open class D : C()

class Z : D() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("")               != "Z" -> "Fail #1"
        (z : D).foo("")         != "Z" -> "Fail #2"
        (z : C).foo("")         != "Z" -> "Fail #3"
        (z : B<String>).foo("") != "Z" -> "Fail #4"
        (z : A<String>).foo("") != "Z" -> "Fail #5"
        else -> "OK"
    }
}
