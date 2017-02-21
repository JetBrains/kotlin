interface A<T> {
    fun foo(t: T): String
}

interface B {
    fun foo(t: Int) = "B"
}

class Z : B

class Z1 : A<Int>, B by Z()

fun box(): String {
    val z1 = Z1()
    val z1a: A<Int> = z1
    val z1b: B = z1

    return when {
        z1.foo( 0)  != "B" -> "Fail #1"
        z1a.foo( 0) != "B" -> "Fail #2"
        z1b.foo( 0) != "B" -> "Fail #3"
        else -> "OK"
    }
}

fun main(args: Array<String>) {
    println(box())
}