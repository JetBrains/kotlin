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

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: SetAlias.kt
package library3

typealias SetAlias<A> = Set<A>

// MODULE: library4(library1, library2, library3)
// MODULE_KIND: LibraryBinary
// FILE: MyInterface.kt
package library4

import library1.*
import library2.*
import library3.*

interface MyInterface {
    fun check(list: ListAlias<SetAlias<StringAlias>>)
}

// MODULE: main(library4, library2, library3)
// FILE: main.kt
package main

import library4.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
