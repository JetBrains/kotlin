// FILE: main.kt
package test

import dependency.FooAliasOne
import dependency.FooAliasTwo

fun constructorCall(): Any {
    return FooAliasOne()
}

fun constructorReference(): Any {
    return ::FooAliasTwo
}

// FILE: dependency.kt
package dependency

class Foo
typealias FooAliasOne = Foo
typealias FooAliasTwo = Foo