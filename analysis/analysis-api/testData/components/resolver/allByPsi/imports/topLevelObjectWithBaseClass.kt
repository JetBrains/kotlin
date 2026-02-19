// FILE: dependency.kt
package one.two

abstract class BaseClass {
    class NestedClass

    object NestedObject {
        class NestedClass
        fun foo() {}
        val bar: String = "bar"
    }

    fun baseFoo() {}
    val baseBar: String = "bar"
}

object TopLevelObject : BaseClass() {
    fun foo() {}
    val bar: String = "bar"
}

// FILE: main.kt
package main

import one.two.BaseClass.NestedClass
import one.two.BaseClass.NestedObject

import one.two.BaseClass.NestedObject.NestedClass
import one.two.BaseClass.NestedObject.foo
import one.two.BaseClass.NestedObject.bar

import one.two.TopLevelObject
import one.two.TopLevelObject.baseFoo
import one.two.TopLevelObject.baseBar
import one.two.TopLevelObject.foo
import one.two.TopLevelObject.bar
