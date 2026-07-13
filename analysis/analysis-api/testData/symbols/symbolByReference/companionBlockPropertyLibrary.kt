// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1

    companion {
        val static1: Boolean get() = true
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    Foo.sta<caret>tic1
}
