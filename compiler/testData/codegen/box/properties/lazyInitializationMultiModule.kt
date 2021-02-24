// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// MODULE: lib1
// FILE: lib.kt
var log = ""
val a = 1.also { log += "a" }
val b = 2.also { log += "b" }

// MODULE: main(lib1)
// FILE: main.kt
fun box(): String = if (log + a == "ab1") "OK" else "fail"
