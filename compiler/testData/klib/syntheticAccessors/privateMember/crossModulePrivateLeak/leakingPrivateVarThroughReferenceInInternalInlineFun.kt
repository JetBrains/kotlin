// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR
// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 22

    internal inline fun internalInlineFunction() = ::privateVar
}

// MODULE: main()(lib)
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
