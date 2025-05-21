// IGNORE_BACKEND: JS_IR
// ^^^ Should be fixed by KT-76093

// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 22

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
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
