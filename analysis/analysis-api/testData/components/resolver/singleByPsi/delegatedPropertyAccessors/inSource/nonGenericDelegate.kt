// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

class NonGenericHolder(val data: Any)

operator fun <T> NonGenericHolder.getValue(thisRef: Any?, property: KProperty<*>): T {
    @Suppress("UNCHECKED_CAST")
    return data as T
}

operator fun <T> NonGenericHolder.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
}

var prop: String <expr>by NonGenericHolder("world")</expr>
