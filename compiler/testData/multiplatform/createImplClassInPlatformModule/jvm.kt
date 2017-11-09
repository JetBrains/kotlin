actual class Foo(x: Int) {
    constructor() : this(0)

    val x: Int = x
}

val y = Foo(42).x
