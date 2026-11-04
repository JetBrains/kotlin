// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

inline value class ValueClass(val value: String)

companion var ValueClass.static1: Int
    get() = 0
    set(value) {}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    ValueClass.sta<caret>tic1
}
