// LANGUAGE: +CompanionBlocks +CompanionExtensions
package test

class C {
    companion {
        fun foo() {}
        val bar = 1
    }
}

typealias TA = C

fun usage() {
    TA.foo()
    TA.bar

    TA::foo
}
