// LANGUAGE: +CompanionBlocks
// MODULE: lib
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1

    companion {
        fun static1() {}
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    F<caret>oo.static1()
}
