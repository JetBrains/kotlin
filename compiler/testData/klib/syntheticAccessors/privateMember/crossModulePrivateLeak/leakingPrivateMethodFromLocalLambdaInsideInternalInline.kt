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
