// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    companion {
        fun foo() {}
    }
}

fun usage() {
    // A companion block alone (without a `companion object`) does not introduce a navigable `Companion` qualifier.
    C.Companion.foo()
}
