// FILE: A.kt
class A constructor(val s: String) {
    private val privateVal: String = s

    internal inline fun internalInlineFunction() = ::privateVal

    private inline fun privateInlineFunction() = ::privateVal
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// FILE: main.kt
fun box(): String {
    return A("O").internalInlineFunction().invoke() + A("K").transitiveInlineFunction().invoke()
}
