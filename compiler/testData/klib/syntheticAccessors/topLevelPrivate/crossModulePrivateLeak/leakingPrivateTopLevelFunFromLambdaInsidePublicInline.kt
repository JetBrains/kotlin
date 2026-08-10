// JS_STANDALONE
// ^^^ this test's name is long enough that the per-module JS artifact of its `lib` module, whose file name
// repeats the grouping-escaped module name, exceeds the 255-byte file name limit
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
