// LANGUAGE: +CompanionBlocksAndExtensions
class A

private fun o() = "O"

private val k = "K"

internal inline companion val A.ok
    get() = o() + k

fun box(): String {
    return A.ok
}
