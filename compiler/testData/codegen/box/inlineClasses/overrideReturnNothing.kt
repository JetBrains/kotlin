// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// IGNORE_BACKEND: JS, JS_IR, WASM
// TODO: Fir2Ir generates overrides as finals.
// IGNORE_BACKEND_FIR: JVM_IR

@JvmInline
value class Inlined(val value: Int)

sealed interface A {
    val property: Inlined?

    val property2: Inlined

    fun foo(): Inlined?

    fun foo2(): Inlined
}

class B : A {
    override val property: Nothing? = null

    override val property2: Nothing
        get() = error("OK")

    override fun foo(): Nothing? = null

    override fun foo2(): Nothing = error("OK")
}

fun box(): String {
    val a: A = B()
    if (a.property != null) return "FAIL 1"
    if (a.foo() != null) return "FAIL 2"
    try {
        a.property2
        return "FAIL 3"
    } catch (e: IllegalStateException) {
        if (e.message != "OK") return "FAIL 4: ${e.message}"
    }
    try {
        a.foo2()
        return "FAIL 5"
    } catch (e: IllegalStateException) {
        if (e.message != "OK") return "FAIL 6: ${e.message}"
    }
    return "OK"
}

