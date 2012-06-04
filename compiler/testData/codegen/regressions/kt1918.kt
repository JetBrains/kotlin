class Bar {
}

trait Foo {
    fun xyzzy(x: Any?): String
}

fun buildFoo(bar: Bar.() -> Unit): Foo {
    return object : Foo {
        override fun xyzzy(x: Any?): String {
           (x as? Bar)?.bar()
            return "OK"
        }
    }
}

fun box(): String {
    val foo = buildFoo({})
    return foo.xyzzy(Bar())
}
