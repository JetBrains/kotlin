// LANGUAGE: +CompanionBlocks +CompanionExtensions

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package test

class Owner {
    companion {
        val prop: Int = 1
        fun foo(): Int = prop
    }
}

companion fun Owner.ext(): Int = Owner.foo()

// MODULE: main(lib)
// FILE: main.kt
import test.Owner
import test.ext

fun test(): Int = Owner.foo() + Owner.prop + Owner.ext()
