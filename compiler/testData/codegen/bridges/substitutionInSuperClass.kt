open class A<T> {
    open fun foo(t: T) = "A"
}

open class B : A<String>()

class Z : B() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("")               != "Z" -> "Fail #1"
        (z : B).foo("")         != "Z" -> "Fail #2"
        (z : A<String>).foo("") != "Z" -> "Fail #3"
        else -> "OK"
    }
}
