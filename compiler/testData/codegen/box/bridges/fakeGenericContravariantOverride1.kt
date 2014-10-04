trait A<T> {
    fun foo(t: T): String
}

trait B {
    fun foo(t: Int) = "B"
}

class Z1 : A<Int>, B

class Z2 : B, A<Int>

fun box(): String {
    val z1 = Z1()
    val z2 = Z2()

    return when {
        z1.foo( 0)                 != "B" -> "Fail #1"
        (z1 : A<Int>).foo( 0)      != "B" -> "Fail #2"
        (z1 : B).foo( 0)           != "B" -> "Fail #3"
        z2.foo( 0)                 != "B" -> "Fail #4"
        (z2 : A<Int>).foo( 0)      != "B" -> "Fail #5"
        (z2 : B).foo( 0)           != "B" -> "Fail #6"
        else -> "OK"
    }
}