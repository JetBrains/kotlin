// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class A { V }

fun box(): String {
  val a: A = A.V
  val b: Boolean
  when (a) {
    A.V -> b = true
  }
  return if (b) "OK" else "FAIL"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
