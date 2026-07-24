// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: lib
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1
}

companion var Foo.static1: Boolean
    get() = true
    set(value) {

    }

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    Foo.sta<caret>tic1 = false
}
