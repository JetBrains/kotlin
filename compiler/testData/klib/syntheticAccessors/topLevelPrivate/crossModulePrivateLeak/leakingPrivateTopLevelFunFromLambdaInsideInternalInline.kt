// MODULE: lib
// FILE: A.kt
private fun privateMethod() = "OK"

internal inline val internalInlineVal: () -> String
    get() = { privateMethod() }


// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineVal.invoke()
}
