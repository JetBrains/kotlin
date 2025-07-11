// IGNORE_BACKEND: JVM_IR
// The test should be unmuted for JVM when KT-77870 issue is fixed.

// MODULE: lib
// FILE: A.kt
private var privateVar: String = ""

internal inline fun internalInlineFunction() = ::privateVar

private inline fun privateInlineFunction() = ::privateVar
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().apply { set("O") }.get() + transitiveInlineFunction().apply { set("K") }.get()
}
