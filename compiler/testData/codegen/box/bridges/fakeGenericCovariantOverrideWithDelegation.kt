// IGNORE_BACKEND_FIR: JVM_IR
interface A<T> {
    fun foo(t: T): String
}

interface B {
    fun foo(t: Int) = "B"
}

class Z : B

class Z1 : A<Int>, B by Z()

class Z2 : B by Z(), A<Int>

fun box(): String {
    val z1 = Z1()
    val z2 = Z2()
    val z1a: A<Int> = z1
    val z1b: B = z1
    val z2a: A<Int> = z2
    val z2b: B = z2

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