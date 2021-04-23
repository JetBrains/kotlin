// IGNORE_BACKEND_FIR: JVM_IR
// MODULE: lib
// FILE: lib.kt
@PublishedApi
internal fun published() = "OK"

inline fun test() = published()

// MODULE: main(lib)
// FILE: main.kt
fun box() = test()
