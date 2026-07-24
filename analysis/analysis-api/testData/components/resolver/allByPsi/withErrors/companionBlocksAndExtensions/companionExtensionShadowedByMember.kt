// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    companion {
        fun foo() {}
    }
}

companion fun C.foo() {}

fun usage() {
    C.foo()
}
