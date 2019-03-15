// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Foo {
    val a: Int = 42
    val b by Delegate(0)
}

inline class Delegate(val ignored: Int): ReadOnlyProperty<Foo, Int> {
    override fun getValue(thisRef: Foo, property: KProperty<*>): Int {
        return thisRef.a
    }
}

fun box(): String {
    val x = Foo()
    if (x.b != 42) throw AssertionError()

    return "OK"
}