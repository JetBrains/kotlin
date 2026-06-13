// LANGUAGE: -CompanionBlocksAndExtensions
package test

class C {
    companion {
        fun foo() {}
    }
}

fun usage() {
    C.foo()
}
