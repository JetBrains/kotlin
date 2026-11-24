// LANGUAGE: +CompanionBlocks
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

enum class E {
    A, B;

    fun regular() {}

    companion {
        fun fromBlock(): E = A
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun test(e: lib.<caret>E) {}
