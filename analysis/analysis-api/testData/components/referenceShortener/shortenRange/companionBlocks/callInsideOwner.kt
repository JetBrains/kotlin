// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
package test

class Owner {
    companion {
        fun foo(): Int = 1
        val prop: Int = 2
    }

    fun bar(): Int = <expr>test.Owner.foo() + test.Owner.prop</expr>
}
