// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: a.kt
private fun privateFun() = "OK"

internal inline fun internalInlineFun() = privateFun()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun().toString()
}
