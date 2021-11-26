// WITH_STDLIB

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Foo {
    val a: Int = 42
    val b by Delegate(0)
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Delegate(val ignored: Int): ReadOnlyProperty<Foo, Int> {
    override fun getValue(thisRef: Foo, property: KProperty<*>): Int {
        return thisRef.a
    }
}

fun box(): String {
    val x = Foo()
    if (x.b != 42) throw AssertionError()

    return "OK"
}