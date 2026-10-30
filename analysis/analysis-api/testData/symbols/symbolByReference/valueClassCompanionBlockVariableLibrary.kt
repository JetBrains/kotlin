// LANGUAGE: +CompanionBlocks
// WITH_STDLIB
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

@JvmInline
value class ValueClass(val value: String) {
    companion {
        var static1: Int = 0
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    ValueClass.sta<caret>tic1
}
