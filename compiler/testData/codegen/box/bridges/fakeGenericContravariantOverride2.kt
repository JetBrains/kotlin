interface A<T> {
    fun foo(t: T): String
}

interface B<T : Number> {
    fun foo(a: T) = "B"
}

class Z1 : A<Int>, B<Int>

class Z2 : B<Int>, A<Int>

fun box(): String {
    val z1 = Z1()
    val z2 = Z2()
    val z1a: A<Int> = z1
    val z1b: B<Int> = z1
    val z2a: A<Int> = z2
    val z2b: B<Int> = z2

    return when {
        z1.foo( 0)  != "B" -> "Fail #1"
        z1a.foo( 0) != "B" -> "Fail #2"
        z1b.foo( 0) != "B" -> "Fail #3"
        z2.foo( 0)  != "B" -> "Fail #4"
        z2a.foo( 0) != "B" -> "Fail #5"
        z2b.foo( 0) != "B" -> "Fail #6"
        else -> "OK"
    }
}