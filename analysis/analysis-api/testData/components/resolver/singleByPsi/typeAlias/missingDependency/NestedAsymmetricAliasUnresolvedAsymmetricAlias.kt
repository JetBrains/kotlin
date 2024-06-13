// IGNORE_FE10

// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: BaseAliases.kt
package library1

typealias IntAlias = Int
typealias BooleanAlias = Boolean

// MODULE: library2
// MODULE_KIND: LibraryBinary
// FILE: AsymmetricAlias.kt
package library2

typealias AsymmetricAlias<A, B> = String

// MODULE: library3
// MODULE_KIND: LibraryBinary
// FILE: AsymmetricAlias2.kt
package library3

typealias AsymmetricAlias2<A, B> = List<B>

// MODULE: library4(library1, library2, library3)
// MODULE_KIND: LibraryBinary
// FILE: MyInterface.kt
package library4

import library1.*
import library2.*
import library3.*

interface MyInterface {
    fun check(list: AsymmetricAlias2<IntAlias, AsymmetricAlias<BooleanAlias, IntAlias>>)
}

// MODULE: main(library4, library1, library3)
// FILE: main.kt
package main

import library4.*

fun checkTypeAlias(m: MyInterface) {
    m.c<caret>heck(emptyList())
}
