// LANGUAGE: +CompanionBlocks +CompanionExtensions
class A

private fun A.o() = "O"

private val A.k
    get() = "K"

internal inline companion fun A.internalInlineFun() = A().o() + A().k

fun box(): String {
    return A.internalInlineFun()
}
