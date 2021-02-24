// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
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
