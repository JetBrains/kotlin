
class Bar {
    operator fun invoke(): Foo { return this }

}

fun x() {

}

class Foo {


    operator fun Bar.invoke(): Foo { return this }

    val x: Bar = Bar()

    fun bar() = x() // Should resolve to invoke
}