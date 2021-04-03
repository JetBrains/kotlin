
class Bar {
    operator fun invoke(): Foo { return <!RETURN_TYPE_MISMATCH!>this<!> } // (1)

}

fun x() {

}

class Foo {


    operator fun Bar.invoke(): Foo { return <!RETURN_TYPE_MISMATCH!>this<!> } // (2)

    val x: Bar = Bar()

    fun bar() = x() // Should resolve to invoke (1)
}
