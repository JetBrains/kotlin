// IGNORE_BACKEND: JS_IR

// FILE: A.kt
private fun privateMethod() = "OK"

internal inline val internalInlineVal: () -> String
    get() = { privateMethod() }


// FILE: main.kt
fun box(): String {
    return internalInlineVal.invoke()
}
