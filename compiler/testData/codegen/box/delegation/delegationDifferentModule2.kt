// JVM_ABI_K1_K2_DIFF: KT-63828

// MODULE: lib
// FILE: lib.kt

interface I {
    val bar: Int
}

class Impl : I {
    override val bar: Int = 42
}

class D1(foo: I) : I by foo

// MODULE: main(lib)
// FILE: main.kt

class D2(foo: I) : I by foo

fun box() : String {
    val c = Impl()
    if (D1(c).bar != 42) return "FAIL 1"
    if (D2(c).bar != 42) return "FAIL 2"
    return "OK"
}
