// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
var global = 0;

class C {
  companion object {
      init {
        global = 1;
      }
  }
}

fun box(): String {
  if (global != 0) {
    return "fail1: global = $global"
  }

  val c = C()
  if (global == 1) return "OK" else return "fail2: global = $global"
}

