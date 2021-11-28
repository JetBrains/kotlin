// IGNORE_BACKEND: JS
// IGNORE_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt

val o = "O"

// FILE: B.kt
val ok = o + k

// FILE: C.kt

val k = "K"

// FILE: main.kt

fun box(): String = ok