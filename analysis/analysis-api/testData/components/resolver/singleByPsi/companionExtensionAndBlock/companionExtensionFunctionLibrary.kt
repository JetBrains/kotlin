// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1
}

companion fun Foo.static1() {}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    Foo.sta<caret>tic1()
}
