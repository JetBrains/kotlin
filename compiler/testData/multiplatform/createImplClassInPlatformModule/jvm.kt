actual class Foo(x: Int) {
    actual constructor() : this(0)

    val x: Int = x
}

val y = Foo(42).x
