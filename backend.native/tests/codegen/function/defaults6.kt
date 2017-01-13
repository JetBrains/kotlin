open class Foo(val x: Int = 42)
class Bar : Foo()

fun main(args: Array<String>) {
    println(Bar().x)
}