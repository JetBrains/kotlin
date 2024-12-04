// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: aliases.kt
package library1

typealias StringAlias = String

typealias ListAlias<A> = List<A>

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: ListAlias<StringAlias> = listOf()

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
