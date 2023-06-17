// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_STDLIB

@Suppress("OPT_IN_USAGE_ERROR") // ExperimentalNativeApi is defined only in Native
fun box(): String {
  kotlin.assert(true)
  return "OK"
}
