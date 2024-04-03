// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias StringAlias = String

typealias IntAlias = Int

typealias FunctionAlias<A, B> = (A) -> B

val propertyWithExpandedType: FunctionAlias<StringAlias, IntAlias> = { it.length }

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
