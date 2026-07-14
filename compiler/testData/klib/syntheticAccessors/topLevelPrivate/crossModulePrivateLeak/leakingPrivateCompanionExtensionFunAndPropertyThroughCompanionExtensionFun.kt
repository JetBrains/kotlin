// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: ANY
// MODULE: lib
// FILE: A.kt
class A

private companion fun A.o() = "O"

private companion val A.k = "K"

internal inline companion fun A.internalInlineFun() = o() + k

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A.internalInlineFun()
}
