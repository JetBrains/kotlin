// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: A.kt
internal class A {
    private fun privateMethod() = "OK"

    inline fun inlineFunction() = privateMethod()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().inlineFunction()
}
