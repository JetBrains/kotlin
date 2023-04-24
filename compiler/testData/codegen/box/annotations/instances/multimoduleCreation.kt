// TARGET_BACKEND: JVM_IR
// IGNORE_DEXING
// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// MODULE: lib
// FILE: lib.kt

package a

import kotlin.reflect.KClass

annotation class A(val kClass: KClass<*> = Int::class)

annotation class OtherArrays(
    val doublesArray: DoubleArray = [],
    val enumArray: Array<kotlin.text.RegexOption> = [],
    val annotationsArray: Array<JvmStatic> = [],
    val namesArray: Array<JvmName> = [JvmName("foo")]
)

annotation class UnsignedValue(
    val uint: UInt = 2147483657U // Int.MAX_VALUE + 10
)

annotation class Outer(
    val array: Array<Inner> = [Inner(1), Inner(2)]
) {
    annotation class Inner(val v: Int = 0)
}

// MODULE: app(lib)
// FILE: app.kt

// kotlin.Metadata: IntArray, Array<String>
// kotlin.Deprecated: Nested annotation, enum instance
// a.A: KClass
// a.OtherArrays: Arrays of enums and other annotations

package test

import a.*
import kotlin.test.*

class C {
    fun one(): A = A()
    fun two(): Metadata = Metadata()
    fun three(): Deprecated = Deprecated("foo")
    fun four(): OtherArrays = OtherArrays()
    fun five(): UnsignedValue = UnsignedValue()
    fun six(): Outer = Outer()
}

fun box(): String {
    val a = C().one()
    assertEquals(Int::class, a.kClass)
    assertEquals(
        """@kotlin.Metadata(bytecodeVersion=[1, 0, 3], data1=[], data2=[], extraInt=0, extraString=, kind=1, metadataVersion=[], packageName=)""",
        C().two().toString()
    )
    assertEquals(
        """@kotlin.Deprecated(level=WARNING, message=foo, replaceWith=@kotlin.ReplaceWith(expression=, imports=[]))""",
        C().three().toString()
    )
    val otherArraysStr = C().four().toString()
    // K1 and K2 have different properties order after metadata deserialization
    assertTrue(
        otherArraysStr == """@a.OtherArrays(doublesArray=[], enumArray=[], annotationsArray=[], namesArray=[@kotlin.jvm.JvmName(name=foo)])""" ||
        otherArraysStr == """@a.OtherArrays(annotationsArray=[], doublesArray=[], enumArray=[], namesArray=[@kotlin.jvm.JvmName(name=foo)])"""
    )
    assertEquals(Int.MAX_VALUE.toUInt() + 10.toUInt(), C().five().uint)
    assertEquals("""@a.Outer(array=[@a.Outer.Inner(v=1), @a.Outer.Inner(v=2)])""", C().six().toString())
    return "OK"
}
