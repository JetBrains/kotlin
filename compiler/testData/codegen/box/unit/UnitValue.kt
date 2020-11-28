// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun foo() {}

fun box(): String {
  return if (foo() == Unit) "OK" else "Fail"
}
