open class A<T : Number> {
    open fun foo(t: T) = "A"
}

open class B : A<Int>()

class Z : B() {
    override fun foo(t: Int) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo(0)            != "Z" -> "Fail #1"
        (z : B).foo(0)      != "Z" -> "Fail #2"
        (z : A<Int>).foo(0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
