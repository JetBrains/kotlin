// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: A.kt
class A

private fun o() = "O"

private val k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
