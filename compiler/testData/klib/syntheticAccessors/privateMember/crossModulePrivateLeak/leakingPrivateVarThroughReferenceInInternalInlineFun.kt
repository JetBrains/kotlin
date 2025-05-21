// KT-76093: Support partial linkage for names of property references
// IGNORE_BACKEND: JS_IR
// KT-72862: Undefined symbols
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

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
