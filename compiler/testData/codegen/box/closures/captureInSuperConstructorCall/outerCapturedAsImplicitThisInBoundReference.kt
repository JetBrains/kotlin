// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
abstract class Base(val fn: () -> String)

class Outer {
    val ok = "OK"

    inner class Inner : Base(::ok)
}

fun box() = Outer().Inner().fn()
