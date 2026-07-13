// LANGUAGE: +CompanionBlocksAndExtensions
// MODULE: lib
// FILE: lib.kt

class Foo {
    fun regularFunction() {

    }

    val regularProperty = 1

    fun static1() {}

    companion {
        fun static1() {}
        fun static1(i: Int) {}
        fun static2() {}
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun usage() {
    Foo.sta<caret>tic1()
}
