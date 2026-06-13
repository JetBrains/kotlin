// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C {
    companion {
        companion {
            fun inner() {}
        }

        fun outer() {}
    }
}

fun usage() {
    C.outer()
}
