// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: NATIVE
var global = "A"

class C {
  init {
      global += "D"
  }

  companion object {
      init {
        global += "B"
      }

      init {
          global += "C"
      }
  }
}

fun box(): String {
  if (global != "A") {
    return "fail1: global = $global"
  }

  val c = C()
  if (global == "ABCD") return "OK" else return "fail2: global = $global"
}

