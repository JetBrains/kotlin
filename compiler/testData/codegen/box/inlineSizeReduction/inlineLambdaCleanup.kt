// FILE: lib.kt
// CHECK_NOT_CALLED: produceOK except=box

fun produceOK() = "OK"

inline fun <T> block(f: () -> T) = f()

// FILE: main.kt
// CHECK_NOT_CALLED: produceOK except=box
fun box(): String = block { produceOK() }