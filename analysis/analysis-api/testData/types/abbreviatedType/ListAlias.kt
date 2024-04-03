// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias StringAlias = String

typealias ListAlias<A> = List<A>

val propertyWithExpandedType: ListAlias<StringAlias> = listOf()

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
