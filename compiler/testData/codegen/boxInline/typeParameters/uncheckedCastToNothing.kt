// IGNORE_INLINER: IR
// IGNORE_BACKEND: WASM

// FILE: 1.kt
inline fun <U> unchecked(any: Any): Any {
    @Suppress("UNCHECKED_CAST")
    val u: U = any as U
    return u as Any
}

// FILE: 2.kt
fun box(): String {
    // in current inline implementation everyting works fine
    // but if we will reify all type parameters, then this example will fail on runtime
    return unchecked<Nothing>("OK").toString()
}