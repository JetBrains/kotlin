// MODULE: base
// FILE: Foo.kt
package base

class Foo

// MODULE: expansion(base)
// FILE: expansion.kt
package expansion

import base.*

typealias FooAlias = Foo

val propertyWithExpandedType: FooAlias = Foo()

// MODULE: main(expansion)
// MODULE_KIND: Source
// FILE: main.kt

// callable: expansion/propertyWithExpandedType
