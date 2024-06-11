// FILE: main.kt
package test

import dependency.Outer.FooAliasOne
import dependency.Outer.FooAliasTwo

fun constructorCall(): Any {
    return FooAliasOne()
}

fun constructorReference(): Any {
    return ::FooAliasTwo
}

// FILE: dependency.kt
package dependency

class Foo

@Suppress("TOPLEVEL_TYPEALIASES_ONLY")
class Outer {
    typealias FooAliasOne = Foo
    typealias FooAliasTwo = Foo
}