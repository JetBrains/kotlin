trait A<T, U> {
    fun foo(t: T, u: U) = "A"
}

class Z<T> : A<T, Int> {
    override fun foo(t: T, u: Int) = "Z"
}

fun box(): String {
    val z = Z<Int>()
    return when {
        z.foo(0, 0)                 != "Z" -> "Fail #1"
        (z : A<Int, Int>).foo(0, 0) != "Z" -> "Fail #2"
        else -> "OK"
    }
}
