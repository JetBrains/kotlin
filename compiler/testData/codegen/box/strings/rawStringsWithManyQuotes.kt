// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
class P(val actual: String, val expected: String)
fun array(vararg s: P) = s

fun box() : String {
  val data = array(
    P("""""", ""),
    P(""""""", "\""),
    P("""""""", "\"\""),
    P(""""""""", "\"\"\""),
    P("""""""""", "\"\"\"\""),
    P("""" """, "\" "),
    P(""""" """, "\"\" "),
    P(""" """", " \""),
    P(""" """"", " \"\""),
    P(""" """""", " \"\"\""),
    P(""" """"""", " \"\"\"\""),
    P(""" """""""", " \"\"\"\"\""),
    P("""" """", "\" \""),
    P(""""" """"", "\"\" \"\"")
  )

  for (i in 0..data.size-1) {
    val p = data[i]
    if (p.actual != p.expected) return "Fail at #$i. actual='${p.actual}', expected='${p.expected}'"
  }

  return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ .. 
