// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias StringAlias = String

typealias TransitiveStringAlias = StringAlias?

val propertyWithExpandedType: TransitiveStringAlias = ""

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
