

class Bar {
    fun FooBar.invoke(): Bar = this
}

class Buz

class FooBar

class Foo {
    val Buz.foobar: Bar get() = Bar()

    fun FooBar.chk(buz: Buz) {
        buz.<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!><!UNRESOLVED_REFERENCE!>foobar<!>()<!>
    }
}
