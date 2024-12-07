// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: aliases.kt
package library1

typealias StringAlias = String

typealias ListAlias<A> = List<A>

typealias SetAlias<A> = Set<A>

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: ListAlias<SetAlias<StringAlias>> = listOf()

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
