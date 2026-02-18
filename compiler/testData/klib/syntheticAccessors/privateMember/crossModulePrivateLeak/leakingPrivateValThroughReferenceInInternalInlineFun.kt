// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// IGNORE_BACKEND: JVM_IR
// The test should be unmuted for JVM when KT-77870 issue is fixed.

// MODULE: lib
// FILE: A.kt
class A constructor(val s: String) {
    private val privateVal: String = s

    internal inline fun internalInlineFunction() = ::privateVal

    private inline fun privateInlineFunction() = ::privateVal
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A("O").internalInlineFunction().invoke() + A("K").transitiveInlineFunction().invoke()
}
