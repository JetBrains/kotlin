// LANGUAGE: +CompanionBlocks +CompanionExtensions
class A

private companion fun A.o() = "O"

private companion val A.k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

fun box(): String {
    return A.internalInlineFun()
}
