// DONT_TARGET_EXACT_BACKEND: JS_IR
// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// ^^^ Muted because accessor for function/constructor/property references are not generated for JS and first stage. To be fixed in KT-69797.
// Can be replaced with ignore after KT-69941

// FILE: A.kt
class A {
    private var privateVar = 22

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateVar
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += publicInlineFunction().get()
        publicInlineFunction().set(20)
        result += publicInlineFunction().get()
    }
    if (result != 42) return result.toString()
    return "OK"
}
