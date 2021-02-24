// IGNORE_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: WASM
// SPLIT_PER_MODULE
// PROPERTY_LAZY_INITIALIZATION

// MODULE: lib1
// FILE: A.kt
val o = "O"

// FILE: B.kt
val okCandidate = o + k

// FILE: C.kt
val k = "K"

// MODULE: lib2(lib1)
// FILE: lib2.kt
val ok = okCandidate

// MODULE: main(lib1, lib2)
// FILE: main.kt
fun box(): String = ok