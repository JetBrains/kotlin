// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package library

class ClassWithImplicitConstructorAndCompanion {
    companion object {

    }
}

// MODULE: main(lib)
// FILE: main.kt
package test

import library.ClassWithImplicitConstructorAndCompanion

fun usage() {
    val a: ClassWit<caret>hImplicitConstructorAndCompanion
}