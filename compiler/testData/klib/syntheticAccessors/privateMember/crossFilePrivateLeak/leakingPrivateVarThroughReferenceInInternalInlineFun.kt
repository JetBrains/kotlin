// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^^^ Muted because accessor for function/constructor/property references are not generated for JS and first stage. To be fixed in KT-69797.
// Can be replaced with ignore after KT-69941

// FILE: A.kt
class A {
    private var privateVar = 22

    internal inline fun internalInlineFunction() = ::privateVar
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += internalInlineFunction().get()
        internalInlineFunction().set(20)
        result += internalInlineFunction().get()
    }
    if (result != 42) return result.toString()
    return "OK"
}
