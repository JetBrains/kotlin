// IGNORE_BACKEND: JVM_IR
// The test should be unmuted for JVM when KT-77870 issue is fixed.

// MODULE: lib
// FILE: A.kt
private val String.privateVal: String
    get() = this

internal inline fun internalInlineFunction() = String::privateVal

private inline fun privateInlineFunction() = String::privateVal
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke("O") + transitiveInlineFunction().invoke("K")
}
