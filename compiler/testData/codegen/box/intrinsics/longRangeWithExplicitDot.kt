// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
  val l: Long = 1
  val l2: Long = 2
  val r = l.rangeTo(l2)
  return if (r.start == l && r.endInclusive == l2) "OK" else "fail"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ rangeTo 
