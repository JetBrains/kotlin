// LANGUAGE: +CompanionBlocks
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

class Owner {
    fun instanceFunction() {}

    companion {
        fun companionBlockFunction() {}

        val companionBlockProperty: Int = 1
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun test(owner: lib.Own<caret>er) {}
