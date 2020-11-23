// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: A.kt

package a

class Box() {
    internal fun result(value: String = "OK"): String = value
}

// FILE: B.kt

fun box(): String {
    return a.Box().result()
}
