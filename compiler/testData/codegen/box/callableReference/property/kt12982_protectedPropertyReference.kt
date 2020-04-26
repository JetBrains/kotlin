class Foo {
    protected var x = 0

    fun getX() = Foo::x
}

fun box(): String {
    val x = Foo().getX()
    val foo = Foo()
    x.set(foo, 42)
    return if (x.get(foo) == 42) "OK" else "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
