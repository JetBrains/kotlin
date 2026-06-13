// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: lib
// FILE: A.kt
class A

private companion fun A.o() = "O"

private companion val A.k = "K"

private companion var A.empty = "!"

internal inline fun internalInlineFun(): String {
    A.empty = ""
    return A.o() + A.empty + A.k
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
