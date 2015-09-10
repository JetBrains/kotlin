interface Foo {
    protected class Bar {
        fun box() = "OK"
    }
}

class Baz : Foo {
    fun box() = Foo.Bar().box()
}

fun box() = Baz().box()
