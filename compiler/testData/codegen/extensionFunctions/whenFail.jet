fun StringBuilder.takeFirst(): Char {
  if (this.length() == 0) return 0.toChar()
  val c = this.charAt(0)
  this.deleteCharAt(0)
  return c
}

fun foo(expr: StringBuilder): Int {
  val c = expr.takeFirst()
  when(c) {
    0.toChar() -> throw Exception("zero")
    else -> throw Exception("nonzero" + c)
  }
}
