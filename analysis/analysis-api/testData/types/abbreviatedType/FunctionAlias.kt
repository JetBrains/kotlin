// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias FunctionAlias = (String) -> Int

val propertyWithExpandedType: FunctionAlias = { it.length }

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
