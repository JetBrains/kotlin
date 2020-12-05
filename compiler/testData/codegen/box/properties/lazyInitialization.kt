// IGNORE_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt

val o = "O"

// FILE: B.kt
val ok = o + k

// FILE: C.kt

val k = "K"

// FILE: main.kt

fun box(): String = ok