// LANGUAGE: +CompanionBlocks
// MODULE: lib
// FILE: lib.kt
package p

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
    p.Foo.sta<caret>tic1
}
