trait A<T> {
    fun foo(t: T): String
}

trait B<T : Number> {
    fun foo(a: T) = "B"
}

class Z1 : A<Int>, B<Int>

class Z2 : B<Int>, A<Int>

fun box(): String {
    val z1 = Z1()
    val z2 = Z2()

    return when {
        z1.foo( 0)                 != "B" -> "Fail #1"
        (z1 : A<Int>).foo( 0)      != "B" -> "Fail #2"
        (z1 : B<Int>).foo( 0)      != "B" -> "Fail #3"
        z2.foo( 0)                 != "B" -> "Fail #4"
        (z2 : A<Int>).foo( 0)      != "B" -> "Fail #5"
        (z2 : B<Int>).foo( 0)      != "B" -> "Fail #6"
        else -> "OK"
    }
}