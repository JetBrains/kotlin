abstract class Base(val fn: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(::ok)
}

fun box() = Outer().Inner().fn()

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES