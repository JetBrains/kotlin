// MODULE: lib
// FILE: A.kt
private fun privateFun() = "OK"

internal inline fun internalInlineFunction() = ::privateFun

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke()
}
