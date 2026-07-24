// LANGUAGE: +CompanionBlocks
// MODULE: lib
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1

    companion {
        var static1: Boolean = true
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    Foo.sta<caret>tic1 = false
}
