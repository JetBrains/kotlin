// IGNORE_BACKEND_FIR: JVM_IR
class Bar {
}

interface Foo {
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
