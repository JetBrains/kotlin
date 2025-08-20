// FILE: A.kt
class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(s: String) = Inner(s)
    }
}

// FILE: main.kt
fun box(): String {
    return A().Inner().internalInlineMethod("OK").s
}
