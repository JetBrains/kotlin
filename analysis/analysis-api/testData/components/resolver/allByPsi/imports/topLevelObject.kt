// FILE: dependency.kt
package one.two

object TopLevelObject {
    class NestedClass

    object NestedObject {
        class NestedClass
        fun foo() {}
        val bar: String = "bar"
    }

    fun foo() {}
    val bar: String = "bar"
}

// FILE: main.kt
package main

import one.two.TopLevelObject.NestedClass
import one.two.TopLevelObject.NestedObject
import one.two.TopLevelObject.foo
import one.two.TopLevelObject.bar

import one.two.TopLevelObject.NestedObject.NestedClass
import one.two.TopLevelObject.NestedObject.foo
import one.two.TopLevelObject.NestedObject.bar
