// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

operator fun Int.plus(s: String) : String {
  System.out?.println("Int.plus(s: String) called")
  return s
}

fun box() : String {
   val s = "${1 + "a"}"
   return if(s == "a") "OK" else "fail"
}
