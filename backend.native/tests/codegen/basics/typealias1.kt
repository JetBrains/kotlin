fun main(args: Array<String>) {
    println(Bar(42).x)
}

class Foo(val x: Int)
typealias Bar = Foo