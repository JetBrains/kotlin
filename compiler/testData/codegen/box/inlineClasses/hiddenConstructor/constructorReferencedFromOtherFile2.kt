// WITH_RUNTIME
// FILE: 2.kt

fun box(): String = X(Z("OK")).z.result

// FILE: 1.kt

@JvmInline
value class Z(val result: String)

class X(val z: Z)
