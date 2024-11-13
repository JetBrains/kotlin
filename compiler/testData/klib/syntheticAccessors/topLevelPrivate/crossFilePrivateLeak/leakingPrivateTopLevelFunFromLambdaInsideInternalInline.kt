// IGNORE_INLINER_K2: IR

// FILE: A.kt
private fun privateMethod() = "OK"

internal inline val internalInlineVal: () -> String
    get() = { privateMethod() }


// FILE: main.kt
fun box(): String {
    return internalInlineVal.invoke()
}
