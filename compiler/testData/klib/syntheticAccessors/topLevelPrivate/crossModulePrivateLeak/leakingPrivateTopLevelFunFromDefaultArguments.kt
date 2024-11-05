// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: a.kt
private fun o() = "O"
private fun k() = "K"

internal inline fun internalInlineFun(oo: String = o(), kk: () -> String = { k() }) = oo + kk()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
