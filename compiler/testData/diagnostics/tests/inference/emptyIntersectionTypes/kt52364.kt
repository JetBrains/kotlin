// FIR_IDENTICAL
// WITH_STDLIB

import kotlin.reflect.KProperty

class FieldStyle2(val index: Int? = 0)

interface Foo {
    fun <Type : Comparable<*>> getProperty(): Type? = null
}

class A<T : Foo> {
    fun foo(thisRef: T, property: KProperty<*>): FieldStyle2 {
        return FieldStyle2(thisRef.getProperty())
    }
}