// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
class C {
    fun ffff(i: Int, s: String = "OK") = s
}

fun box(): String = 42.run(C()::ffff)
