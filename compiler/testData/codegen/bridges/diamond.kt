trait A<T, U> {
    fun foo(t: T, u: U) = "A"
}

trait B<U> : A<String, U>

trait C<T> : A<T, Int>

class Z : B<Int>, C<String> {
    override fun foo(t: String, u: Int) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo("", 0)                    != "Z" -> "Fail #1"
        (z : C<String>).foo("", 0)      != "Z" -> "Fail #2"
        (z : B<Int>).foo("", 0)         != "Z" -> "Fail #3"
        (z : A<String, Int>).foo("", 0) != "Z" -> "Fail #4"
        else -> "OK"
    }
}
