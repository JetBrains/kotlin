// IGNORE_BACKEND: JS, NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM

// FILE: A.kt

val o = "O"

// FILE: B.kt
val ok = o + k

// FILE: C.kt

val k = "K"

// FILE: main.kt

fun box(): String = ok