// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    internal inline val internalInlineVal: () -> String
        get() = { privateMethod() }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineVal.invoke()
}
