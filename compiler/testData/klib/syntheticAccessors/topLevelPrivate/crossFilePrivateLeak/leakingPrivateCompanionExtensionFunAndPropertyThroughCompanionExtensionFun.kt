// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: A.kt
class A

private companion fun A.o() = "O"

private companion val A.k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
