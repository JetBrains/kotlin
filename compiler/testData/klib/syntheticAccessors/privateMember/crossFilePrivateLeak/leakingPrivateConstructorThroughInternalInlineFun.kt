// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")
    internal inline fun copy(s: String) = A(s)
}

// FILE: main.kt
fun box(): String {
    return A().copy("OK").s
}
