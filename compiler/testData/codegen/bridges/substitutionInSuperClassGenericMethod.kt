open class A<T> {
    open fun <U> foo(t: T, u: U) = "A"
}

open class B : A<String>()

class Z : B() {
    override fun <U> foo(t: String, u: U) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("", 0)               != "Z" -> "Fail #1"
        (z : B).foo("", 0)         != "Z" -> "Fail #2"
        (z : A<String>).foo("", 0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
