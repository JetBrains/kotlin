// LANGUAGE: +CompanionBlocksAndExtensions
package test

object O {
    companion {
        fun foo() {}
    }
}

enum class E {
    Entry {
        companion {
            fun bar() {}
        }
    };
}

fun usage() {
    O.foo()
    E.Entry
}
