// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

public class RunnableFunctionWrapper(val f : () -> Unit) : Runnable {
    public override fun run() {
        f()
    }
}

fun box() : String {
  var res = ""
  RunnableFunctionWrapper({ res = "OK" }).run()
  return res
}
