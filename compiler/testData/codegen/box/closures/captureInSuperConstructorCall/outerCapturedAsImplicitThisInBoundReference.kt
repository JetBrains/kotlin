// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
abstract class Base(val fn: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(::ok)
}

fun box() = Outer().Inner().fn()
