// FILE: main.kt
package test

import dependency.FooAliasOne
import dependency.FooAliasTwo

fun constructorCall(): Any {
    return FooAliasOne { }
}

fun constructorReference(): Any {
    return ::FooAliasTwo
}

// FILE: dependency.kt
package dependency

fun interface Foo {
    fun foo()
}
typealias FooAliasOne = Foo
typealias FooAliasTwo = Foo