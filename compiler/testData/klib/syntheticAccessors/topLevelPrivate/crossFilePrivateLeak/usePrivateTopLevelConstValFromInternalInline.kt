// FILE: A.kt

private const val privateConstVal = "OK"

internal inline fun internalInlineMethod() = privateConstVal

// FILE: main.kt
fun box(): String {
    return internalInlineMethod()
}