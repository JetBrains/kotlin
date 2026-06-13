// LANGUAGE: +CompanionBlocksAndExtensions
class A

private fun o() = "O"

private val k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

fun box(): String {
    return A.internalInlineFun()
}
