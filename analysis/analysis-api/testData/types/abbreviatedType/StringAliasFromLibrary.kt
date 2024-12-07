// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: StringAlias.kt
package library1

typealias StringAlias = String

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: StringAlias = ""

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
