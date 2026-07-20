// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: declarations.kt
package decl

class C

companion fun C.foo() {}
companion val C.prop: Int get() = 1

// FILE: main.kt
package use

import decl.C
import decl.foo
import decl.prop

fun usage() {
    C.foo()
    C.prop
}
