// IGNORE_BACKEND: WASM
fun stringConcat(n : Int) : String? {
  var string : String? = ""
  for (i in 0..(n - 1))
    string += "LOL "
  return string
}

fun box() = if(stringConcat(3) == "LOL LOL LOL ") "OK" else "fail"

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
