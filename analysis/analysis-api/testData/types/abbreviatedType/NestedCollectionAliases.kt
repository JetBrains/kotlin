// MODULE: expansion
// FILE: expansion.kt
package expansion

typealias StringAlias = String

typealias ListAlias<A> = List<A>

typealias SetAlias<A> = Set<A>

val propertyWithExpandedType: ListAlias<SetAlias<StringAlias>> = listOf()

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
