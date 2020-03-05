

class Bar {
    fun FooBar.invoke(): Bar = this
}

class Buz

class FooBar

class Foo {
    val Buz.foobar: Bar get() = Bar()

    fun FooBar.chk(buz: Buz) {
        // NB: really this example is unresolvable (in old FE too)
        // local/buz is extension receiver of foobar
        // this@Foo is dispatch receiver of foobar
        // Foo/foobar is dispatch receiver of invoke
        // this@chk is extension receiver of invoke
        buz.<!UNRESOLVED_REFERENCE!>foobar<!>()
    }
}
