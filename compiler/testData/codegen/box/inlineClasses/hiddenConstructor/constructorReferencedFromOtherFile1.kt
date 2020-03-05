// IGNORE_BACKEND_FIR: JVM_IR
// FILE: 1.kt

fun box(): String = X(Z("OK")).z.result

// FILE: 2.kt

inline class Z(val result: String)

class X(val z: Z)
