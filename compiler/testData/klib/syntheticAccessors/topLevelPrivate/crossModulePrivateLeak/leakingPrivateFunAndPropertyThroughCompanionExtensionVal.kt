// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: ANY
// MODULE: lib
// FILE: A.kt
class A

private fun o() = "O"

private val k = "K"

internal inline companion val A.ok
    get() = o() + k

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A.ok
}
