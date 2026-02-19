// MODULE: library1
// MODULE_KIND: LibraryBinary
// FILE: aliases.kt
package library1

typealias IntAlias = Int

typealias BooleanAlias = Boolean

typealias AsymmetricAlias<A, B> = String

typealias AsymmetricAlias2<A, B> = List<B>

// MODULE: expansion(library1)
// FILE: expansion.kt
package expansion

import library1.*

val propertyWithExpandedType: AsymmetricAlias2<IntAlias, AsymmetricAlias<BooleanAlias, IntAlias>> = listOf()

// MODULE: main(expansion, library1)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
