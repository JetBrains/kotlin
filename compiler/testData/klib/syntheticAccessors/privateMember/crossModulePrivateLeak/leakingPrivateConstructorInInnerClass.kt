// MODULE: lib
// FILE: A.kt
class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(s: String) = Inner(s)
    }
}

// MODULE: main()(lib)
// FILE: B.kt
fun box(): String {
    return A().Inner().internalInlineMethod("OK").s
}
