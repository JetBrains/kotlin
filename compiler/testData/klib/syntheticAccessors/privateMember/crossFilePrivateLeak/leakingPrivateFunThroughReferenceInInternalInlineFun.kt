// FILE: A.kt
private fun privateFun() = "OK"

internal inline fun internalInlineFunction() = ::privateFun

// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke()
}
