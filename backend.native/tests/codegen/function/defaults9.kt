class Foo {
    inner class Bar(x: Int, val y: Int = 1) {
        constructor() : this(42)
    }
}

fun main(arg:Array<String>) = println(Foo().Bar().y)
