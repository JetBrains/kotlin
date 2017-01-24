class Foo {
    fun test(x: Int = 1) = x
}

class Bar {
    fun test(x: Int = 2) = x
}

fun main(args : Array<String>) {
    println(Bar().test())
}