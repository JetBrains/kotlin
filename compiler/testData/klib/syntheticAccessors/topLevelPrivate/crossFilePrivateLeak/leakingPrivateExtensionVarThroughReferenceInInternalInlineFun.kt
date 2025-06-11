// FILE: A.kt
private var privateVar: String = ""

internal inline fun internalInlineFunction() = ::privateVar

private inline fun privateInlineFunction() = ::privateVar
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().apply { set("O") }.get() + transitiveInlineFunction().apply { set("K") }.get()
}
