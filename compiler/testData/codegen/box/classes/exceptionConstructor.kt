// IGNORE_BACKEND: WASM
class GameError(msg: String): Exception(msg) {
}

fun box(): String {
  val e = GameError("foo")
  return if (e.message == "foo") "OK" else "fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ Exception 
