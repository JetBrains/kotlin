// KT-62889

// IGNORE_FE10

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: MyString.kt
package library1

typealias MyString = String

// MODULE: library2(library1)
// MODULE_KIND: LibraryBinary
// FILE: MyInterface.kt
package library2

import library1.*

interface MyInterface {
    fun check(string: MyString)
}

// MODULE: main(library2)
// FILE: main.kt
package main

import library2.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck("")
}
