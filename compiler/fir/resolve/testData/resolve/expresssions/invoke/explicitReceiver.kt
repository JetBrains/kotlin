
fun x() {

}

class Foo {

    val x: Foo = Foo()

    operator fun invoke(): Foo { return this }

    fun bar() = x() // Should resolve to invoke
}

class Bar {
    fun x() {}

    val x: Bar = Bar()

    operator fun invoke(): Bar { return this }

    fun baz() {
        x() // Should resolve to fun x()
    }
}