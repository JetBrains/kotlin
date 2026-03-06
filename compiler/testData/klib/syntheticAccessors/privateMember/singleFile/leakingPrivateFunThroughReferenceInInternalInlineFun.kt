// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
class A {
    private fun privateFun(s: String) = s

    internal inline fun internalInlineFunction() = ::privateFun

    private inline fun privateInlineFunction() = ::privateFun
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

fun box(): String {
    return A().internalInlineFunction().invoke("O") + A().transitiveInlineFunction().invoke("K")
}
