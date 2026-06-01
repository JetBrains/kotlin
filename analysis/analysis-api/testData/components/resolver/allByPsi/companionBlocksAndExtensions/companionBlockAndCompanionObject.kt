// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C {
    companion {
        fun foo() = ""
        val bar = ""
    }

    companion object {
        fun foo() = 1
    }
}

fun usage() {
    C.foo()
    C.bar

    C.Companion.foo()
}
