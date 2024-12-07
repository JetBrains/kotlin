// WITH_REFLECT

package myPack

import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty

class ResourceID<T> {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, T> {
        return MyDelegate()
    }
}

class MyDelegate<T> : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return null as T
    }
}

val providedDelegate by ResourceID<Int>()