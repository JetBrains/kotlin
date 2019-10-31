// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

class Foo {
    var test: String by refreshOnUpdate("str")

    fun <T> refreshOnUpdate(initialValue: T) = RefreshDelegate(initialValue)

    class RefreshDelegate<T>(initialValue: T?) {
        operator fun getValue(thisRef: Foo, property: KProperty<*>): T = TODO()

        operator fun setValue(thisRef: Foo, property: KProperty<*>, value: T) {}
    }
}