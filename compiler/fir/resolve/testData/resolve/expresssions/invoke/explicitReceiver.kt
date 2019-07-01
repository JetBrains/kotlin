
fun x() {

}

class Foo {

    val x: Foo = Foo()

    operator fun invoke(): Foo { return this }

    fun bar() = x() // Should resolve to invoke
}