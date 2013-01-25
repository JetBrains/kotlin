open class A<T : Number> {
    open fun foo(t: T) = "A"
}

class Z : A<Int>() {
    override fun foo(t: Int) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo(0)            != "Z" -> "Fail #1"
        (z : A<Int>).foo(0) != "Z" -> "Fail #2"
        else -> "OK"
    }
}
