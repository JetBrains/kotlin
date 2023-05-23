// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: WASM
// MODULE: lib
// FILE: 2.kt
val a get() = "OK"
val b get() = a

// FILE: 3.kt
val c get() = b

// MODULE: main(lib)
// FILE: 1.kt
val d get() = c

fun box(): String = d

// FILE: 2.kt
val a get() = "OK"
val b get() = a
