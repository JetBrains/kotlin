// IGNORE_BACKEND: WASM
fun box(): String {
  val r = 1.toLong()..2
  var s = ""
  for (l in r) {
    s += l
  }
  return if (s == "12") "OK" else "fail: $s"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
