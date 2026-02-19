// FILE: dependency.kt
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

import TopLevelObject.NestedClass
import TopLevelObject.NestedObject
import TopLevelObject.foo
import TopLevelObject.bar

import TopLevelObject.NestedObject.NestedClass
import TopLevelObject.NestedObject.foo
import TopLevelObject.NestedObject.bar
