// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: a.kt
private fun privateFun() = "OK"

internal inline fun internalInlineFun() = privateFun()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun().toString()
}
