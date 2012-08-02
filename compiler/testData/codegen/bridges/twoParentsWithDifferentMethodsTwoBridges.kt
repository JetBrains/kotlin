trait A<T> {
    fun foo(t: T, u: Int) = "A"
}

trait B<T, U> {
    fun foo(t: T, u: U) = "B"
}

class Z : A<String>, B<String, Int> {
    override fun foo(t: String, u: Int) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("", 0)                    != "Z" -> "Fail #1"
        (z : A<String>).foo("", 0)      != "Z" -> "Fail #2"
        (z : B<String, Int>).foo("", 0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
