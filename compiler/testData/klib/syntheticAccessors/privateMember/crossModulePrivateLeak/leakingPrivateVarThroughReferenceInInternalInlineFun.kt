// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^^^ KT-69797: Muted because accessor for function/constructor/property references are not generated for JS and first stage. To be fixed in KT-69797.
// Can be replaced with ignore after KT-69941

// IGNORE_BACKEND: NATIVE
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: NATIVE
// ^^^ KT-76712, KT-76547: error: <missing declarations>: No function found for symbol '/<unknown name>|?'
//                         error: there are linkage errors reported by the partial linkage engine

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
