trait A<T> {
    fun foo(t: T) = "A"
}

class Z : A<String>


fun box(): String {
    val z = Z()
    return when {
        z.foo("")               != "A" -> "Fail #1"
        (z : A<String>).foo("") != "A" -> "Fail #2"
        else -> "OK"
    }
}
