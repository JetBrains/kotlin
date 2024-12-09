// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681 and KT-71416.

// KT-72862: Undefined symbols
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// FILE: A.kt
class A {
    private inner class Inner {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Inner().foo()
    internal inline fun internalInlineFun() = privateFun()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}