// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: FunctionAlias.kt
package library1

typealias FunctionAlias = (String) -> Int

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: FunctionAlias = { it.length }

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
