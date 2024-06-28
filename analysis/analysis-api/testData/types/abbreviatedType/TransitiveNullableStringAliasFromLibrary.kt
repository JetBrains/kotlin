// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: StringAlias.kt
package library1

typealias StringAlias = String?

typealias TransitiveStringAlias = StringAlias

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: TransitiveStringAlias = ""

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
