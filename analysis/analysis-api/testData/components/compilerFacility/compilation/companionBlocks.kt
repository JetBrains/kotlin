// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: main.kt
class Owner {
    companion {
        val prop: Int = 1
        fun foo(): Int = prop
    }
}

companion fun Owner.ext(): Int = Owner.foo()

fun test(): Int = Owner.foo() + Owner.prop + Owner.ext()
