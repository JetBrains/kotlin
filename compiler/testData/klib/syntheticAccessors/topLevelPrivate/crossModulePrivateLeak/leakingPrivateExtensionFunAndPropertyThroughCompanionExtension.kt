// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: ANY
// MODULE: lib
// FILE: A.kt
class A

private fun A.o() = "O"

private val A.k
    get() = "K"

internal inline companion fun A.internalInlineFun() = A().o() + A().k

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
