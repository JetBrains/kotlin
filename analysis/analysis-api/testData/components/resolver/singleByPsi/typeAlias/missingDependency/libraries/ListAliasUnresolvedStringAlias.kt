// IGNORE_FE10

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: StringAlias.kt
package library1

typealias StringAlias = String

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: ListAlias.kt
package library2

typealias ListAlias<A> = List<A>

// MODULE: library3(library1, library2)
// MODULE_KIND: LibraryBinary
// FILE: MyInterface.kt
package library3

import library1.*
import library2.*

interface MyInterface {
    fun check(list: ListAlias<StringAlias>)
}

// MODULE: main(library3, library2)
// FILE: main.kt
package main

import library3.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
