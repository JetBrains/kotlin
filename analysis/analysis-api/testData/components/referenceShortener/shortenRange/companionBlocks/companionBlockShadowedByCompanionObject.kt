// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
package test

class Owner {
    companion {
        fun foo(): Int = 1
    }

    companion object {
        fun foo(): Int = 2
    }

    fun bar(): Int = <expr>test.Owner.Companion.foo()</expr>
}
