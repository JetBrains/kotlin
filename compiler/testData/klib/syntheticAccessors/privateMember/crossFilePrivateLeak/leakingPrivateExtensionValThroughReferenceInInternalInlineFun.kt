// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// FILE: A.kt
private val String.privateVal: String
    get() = this

internal inline fun internalInlineFunction() = String::privateVal

private inline fun privateInlineFunction() = String::privateVal
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke("O") + transitiveInlineFunction().invoke("K")
}
