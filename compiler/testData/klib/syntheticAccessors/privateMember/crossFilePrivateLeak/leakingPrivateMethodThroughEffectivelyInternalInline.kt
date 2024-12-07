// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// FILE: A.kt
internal class A {
    private fun privateMethod() = "OK"

    inline fun inlineFunction() = privateMethod()
}

// FILE: main.kt
fun box(): String {
    return A().inlineFunction()
}
