// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias IntAlias = Int

typealias BooleanAlias = Boolean

typealias AsymmetricAlias<A, B> = String

typealias AsymmetricAlias2<A, B> = List<B>

val propertyWithExpandedType: AsymmetricAlias2<IntAlias, AsymmetricAlias<BooleanAlias, IntAlias>> = listOf()

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
