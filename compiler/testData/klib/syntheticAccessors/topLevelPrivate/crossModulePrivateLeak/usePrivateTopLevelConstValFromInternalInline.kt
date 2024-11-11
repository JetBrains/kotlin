// MODULE: lib
// FILE: a.kt

private const val privateConstVal = "OK"

internal inline fun internalInlineMethod() = privateConstVal

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineMethod()
}