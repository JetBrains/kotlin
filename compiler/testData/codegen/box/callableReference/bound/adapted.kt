class C {
    fun ffff(i: Int, s: String = "OK") = s
}

fun box(): String = 42.run(C()::ffff)

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
