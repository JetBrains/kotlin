// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE && target=linux_x64
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE && target=linux_x64
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
