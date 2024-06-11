// FILE: main.kt
package test

import dependency.FooAliasOne
import dependency.FooAliasTwo
import dependency.Foo

fun constructorCall(): Any {
    return Foo()
}

fun constructorReference(): Any {
    return ::Foo
}

// FILE: dependency.kt
package dependency

class Foo
typealias FooAliasOne = Foo
typealias FooAliasTwo = Foo