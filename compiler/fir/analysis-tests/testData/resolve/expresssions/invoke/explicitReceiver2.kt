// RUN_PIPELINE_TILL: FRONTEND

class Bar {
    operator fun invoke(): Foo { return <!RETURN_TYPE_MISMATCH!>this<!> } // (1)

}

fun x() {

}

class Foo {


    operator fun Bar.<!EXTENSION_SHADOWED_BY_MEMBER!>invoke<!>(): Foo { return <!RETURN_TYPE_MISMATCH!>this<!> } // (2)

    val x: Bar = Bar()

    fun bar() = x() // Should resolve to invoke (1)
}
