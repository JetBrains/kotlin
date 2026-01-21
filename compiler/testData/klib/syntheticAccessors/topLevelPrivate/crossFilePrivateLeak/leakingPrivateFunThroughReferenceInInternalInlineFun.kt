// FILE: A.kt
private fun privateFun(s: String) = s

internal inline fun internalInlineFunction() = ::privateFun

private inline fun privateInlineFunction() = ::privateFun
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke("O") + transitiveInlineFunction().invoke("K")
}
