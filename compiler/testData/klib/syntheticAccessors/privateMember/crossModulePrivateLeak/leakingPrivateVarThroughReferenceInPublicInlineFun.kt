// IGNORE_BACKEND: ANY
// ^^^ Muted because accessor for function/constructor/property references are not generated. To be fixed in KT-69797.

// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 22

    public inline fun publicInlineFunction() = ::privateVar
}

// MODULE: main(lib)
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
