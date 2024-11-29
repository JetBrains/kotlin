// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: A.kt
fun wrapper(block: () -> Unit) { block() }

private lateinit var o: String

class A {
    internal inline fun inlineMethod(): String {
        lateinit var k: String
        wrapper {
            o = "O"
            k = "K"
        }
        return o + k
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box() = A().inlineMethod()
