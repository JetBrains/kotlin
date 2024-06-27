// IGNORE_BACKEND: ANY

// FILE: A.kt
class A private constructor(val s: String) {
    constructor() : this("")

    inner class Inner {
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        public inline fun copy(s: String) = A(s)
    }
}

// FILE: main.kt
fun box(): String {
    return A().Inner().copy("OK").s
}
