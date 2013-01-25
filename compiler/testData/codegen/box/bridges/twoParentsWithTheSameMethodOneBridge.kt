trait A<T> {
    fun foo(t: T) = "A"
}

trait B<T> {
    fun foo(t: T) = "B"
}

class Z : A<Int>, B<Int> {
    override fun foo(t: Int) = "Z"
}


fun box(): String {
    val z = Z()
    return when {
        z.foo(0)            != "Z" -> "Fail #1"
        (z : A<Int>).foo(0) != "Z" -> "Fail #2"
        (z : B<Int>).foo(0) != "Z" -> "Fail #3"
        else -> "OK"
    }
}
