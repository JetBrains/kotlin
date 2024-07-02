// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: a.kt
private fun String.privateExtension() = "${$this}K"
internal inline fun String.internalInlineMethod() = privateExtension()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return "O".internalInlineMethod()
}
