open class Foo(val a: Int)

class Bar : Foo {
    constructor() : super(<expr>5</expr>) {
        consume("foo")
        consume(100)
    }
}

fun consume(obj: Any) {}