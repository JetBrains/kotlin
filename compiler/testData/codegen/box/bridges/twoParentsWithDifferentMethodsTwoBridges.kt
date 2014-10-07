trait A<T> {
    fun foo(t: T, u: Int) = "A"
}

trait B<T, U> {
    fun foo(t: T, u: U) = "B"
}

class Z1 : A<String>, B<String, Int> {
    override fun foo(t: String, u: Int) = "Z1"
}

class Z2 : B<String, Int>, A<String> {
    override fun foo(t: String, u: Int) = "Z2"
}

fun box(): String {
    val z1 = Z1()
    val z2 = Z2()
    return when {
        z1.foo("", 0)                    != "Z1" -> "Fail #1"
        (z1 : A<String>).foo("", 0)      != "Z1" -> "Fail #2"
        (z1 : B<String, Int>).foo("", 0) != "Z1" -> "Fail #3"
        z2.foo("", 0)                    != "Z2" -> "Fail #4"
        (z2 : A<String>).foo("", 0)      != "Z2" -> "Fail #5"
        (z2 : B<String, Int>).foo("", 0) != "Z2" -> "Fail #6"
        else -> "OK"
    }
}
