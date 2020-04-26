data class A(val x: Unit)

fun box(): String {
    val a = A(Unit)
    return if ("$a" == "A(x=kotlin.Unit)") "OK" else "$a"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT