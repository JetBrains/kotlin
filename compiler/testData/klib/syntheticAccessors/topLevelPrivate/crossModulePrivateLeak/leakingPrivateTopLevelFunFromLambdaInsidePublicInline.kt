// MODULE: lib
// FILE: A.kt
private fun privateMethod() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline val publicInlineVal: () -> String
    get() = { privateMethod() }


// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return publicInlineVal.invoke()
}
