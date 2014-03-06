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
