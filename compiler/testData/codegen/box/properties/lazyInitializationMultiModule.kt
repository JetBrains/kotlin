// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// MODULE: lib1
var log = ""
val a = 1.also { log += "a" }
val b = 2.also { log += "b" }

// MODULE: main(lib1)
fun box(): String = if (log + a == "ab1") "OK" else "fail"