// LANGUAGE: +DataObjects
// WITH_STDLIB

package com.example

import kotlin.test.*

data object DataObject {
    data object Nested
}

class Foo {
    data object Inner
}

data object Declared {
    override fun toString() = "Overriden"
}

open class WithFinalToString {
    final override fun toString() = "FinalToString"
}

data object InheritedFromClassWithFinalToString: WithFinalToString()

open class WithOpenToString {
    override fun toString() = "OpenToString"
}

data object InheritedFromClassWithOpenToString: WithOpenToString()

abstract class WithAbstractToString {
    abstract override fun toString(): String
}

data object InheritedFromClassWithAbstractToString : WithAbstractToString()

class C {
    companion object CC
}

fun box(): String {
    assertEquals("DataObject", DataObject.toString())
    assertEquals("Nested", DataObject.Nested.toString())
    assertEquals("Inner", Foo.Inner.toString())
    assertEquals("Overriden", Declared.toString())
    assertEquals("FinalToString", InheritedFromClassWithFinalToString.toString())
    assertEquals("InheritedFromClassWithOpenToString", InheritedFromClassWithOpenToString.toString())
    assertEquals("InheritedFromClassWithAbstractToString", InheritedFromClassWithAbstractToString.toString())

    return "OK"
}