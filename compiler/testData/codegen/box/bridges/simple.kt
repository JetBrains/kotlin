open class A<T> {
    open fun foo(t: T) = "A"
}

class Z : A<String>() {
    override fun foo(t: String) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("")               != "Z" -> "Fail #1"
        (z : A<String>).foo("") != "Z" -> "Fail #2"
        else -> "OK"
    }
}
