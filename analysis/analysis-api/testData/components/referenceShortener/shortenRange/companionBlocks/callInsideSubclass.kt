// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
package test

open class Owner {
    companion {
        fun foo(): Int = 1
    }
}

class Sub : Owner() {
    fun bar(): Int = <expr>test.Owner.foo()</expr>
}
