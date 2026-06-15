// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JS_IR
// Static property is undefined on JS backend
// FILE: A.kt
class A {
    companion {
        private val o = "O"
        private val k = "K"

        internal inline fun internalInlineStaticMethod() = o
    }

    internal inline fun internalInlineMethod() = k
}

// FILE: main.kt
fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
