// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
package one

enum class E

class C {
    companion {
        fun values() = E.values()
        val entries = E.entries
    }
}

companion fun C.valueOf(x: String) = x

// LIGHT_ELEMENTS_NO_DECLARATION: E.class[getEntries;valueOf;values]
