// IGNORE_BACKEND: WASM
fun box(): String {
  1 in 1.rangeTo(10)
  1..10
  'h' in 'A'.rangeTo('Z')
  return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ rangeTo 
