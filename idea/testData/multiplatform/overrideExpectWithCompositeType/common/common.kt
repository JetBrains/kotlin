expect class Expect

class Box<out T>(val x: T)

interface Base {
    fun expectInReturnType(): Box<Expect>

    fun expectInArgument(e: Box<Expect>)

    fun Box<Expect>.expectInReceiver()

    val expectVal: Box<Expect>

    var expectVar: Box<Expect>
}