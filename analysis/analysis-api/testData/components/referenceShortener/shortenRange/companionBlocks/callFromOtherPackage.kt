// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
package test

fun bar(): Int = <expr>dependency.Owner.foo()</expr>

// FILE: dependency.kt
package dependency

class Owner {
    companion {
        fun foo(): Int = 1
    }
}
