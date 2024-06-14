// IGNORE_BACKEND: NATIVE

// FILE: a.kt
private fun privateFun() = "OK"

internal inline fun internalInlineFun() = privateFun()

// FILE: main.kt
fun box(): String {
    return internalInlineFun().toString()
}
