
class Bar {
    operator fun invoke(): Foo { return this } // (1)

}

fun x() {

}

class Foo {


    operator fun Bar.invoke(): Foo { return this } // (2)

    val x: Bar = Bar()

    fun bar() = x() // Should resolve to invoke (1)
}