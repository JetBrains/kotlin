// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Foo {
    val a: Int = 42
    val b by Delegate(0)
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Delegate<T: Int>(val ignored: T): ReadOnlyProperty<Foo, Int> {
    override fun getValue(thisRef: Foo, property: KProperty<*>): Int {
        return thisRef.a
    }
}

fun box(): String {
    val x = Foo()
    if (x.b != 42) throw AssertionError()

    return "OK"
}