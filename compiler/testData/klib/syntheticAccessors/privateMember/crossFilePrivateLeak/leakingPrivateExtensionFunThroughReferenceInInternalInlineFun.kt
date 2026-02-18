// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// FILE: A.kt
private fun Int.privateExtensionFun(s: String) = s

internal inline fun internalInlineFunction() = Int::privateExtensionFun

private inline fun privateInlineFunction() = Int::privateExtensionFun
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke(1, "O") + transitiveInlineFunction().invoke(1, "K")
}
