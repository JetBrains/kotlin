// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: A.kt

package a

class Box(val value: String) {
    internal fun result(): String = value
}

// FILE: B.kt

fun box(): String {
    return a.Box("OK").result()
}
