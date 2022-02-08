// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: NATIVE

fun StringBuilder.takeFirst(): Char {
  if (this.length == 0) return 0.toChar()
  val c = this.get(0)
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

fun box(): String {
  try {
    foo(StringBuilder())
    return "Fail"
  }
  catch (e: Exception) {
    return "OK"
  }
}
