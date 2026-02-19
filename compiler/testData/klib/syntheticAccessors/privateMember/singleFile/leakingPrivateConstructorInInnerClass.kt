class A {
    inner class Inner private constructor(val s: String) {
        constructor(): this("")

        internal inline fun internalInlineMethod(s: String) = Inner(s)
    }
}

fun box(): String {
    return A().Inner().internalInlineMethod("OK").s
}
