// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String {
  val y = 12
  val op = { x:Int -> (x + y).toString() }

  val op2 : Int.(Int) -> String = { op(this + it) }

  return if("27" == 5.op2(10)) "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ WASM_FUNCTION_REFERENCES_UNSUPPORTED
