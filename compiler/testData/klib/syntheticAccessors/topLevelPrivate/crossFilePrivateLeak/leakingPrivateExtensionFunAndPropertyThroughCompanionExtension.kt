// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: A.kt
class A

private fun A.o() = "O"

private val A.k
    get() = "K"

internal inline companion fun A.internalInlineFun() = A().o() + A().k

// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
