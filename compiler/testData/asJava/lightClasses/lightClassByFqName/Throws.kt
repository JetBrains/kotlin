// Test
// WITH_STDLIB

abstract class Base

class MyException : Exception()

class Test
@Throws(MyException::class)
constructor(
    private val p1: Int
) : Base() {
    @Throws(MyException::class)
    fun readSomething() {
        throw MyException()
    }

    @get:Throws(MyException::class)
    val foo : String = "42"

    val boo : String = "42"
        @Throws(MyException::class)
        get
}
