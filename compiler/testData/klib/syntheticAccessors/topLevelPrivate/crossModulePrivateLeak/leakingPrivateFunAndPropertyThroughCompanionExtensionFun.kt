// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: ANY
// IGNORE_BACKEND: ANY
// Should be unmuted when KT-86578 is fixed
// MODULE: lib
// FILE: A.kt
class A

private fun o() = "O"

private val k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
