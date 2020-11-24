// TARGET_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt
val a1 = "a".let {
    it + "a"
}

object A {
    private val foo = "foo"
    val foo2 = foo
    val ok = "OK"
}

class B(private val foo: String) {
    val ok = foo

    constructor(arg: Int) : this(arg.toString())
}

enum class C {
    OK
}

const val b = "b"

// FILE: main.kt
fun box(): String {
    val foo = A.ok
    val bar = B("foo").ok
    val bay = B(1).ok
    C.OK
    C.values()
    C.valueOf("OK")
    val baz = b
    return if (js("typeof a1") == "undefined") "OK" else "fail"
}