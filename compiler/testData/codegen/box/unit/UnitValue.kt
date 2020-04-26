fun foo() {}

fun box(): String {
  return if (foo() == Unit) "OK" else "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT