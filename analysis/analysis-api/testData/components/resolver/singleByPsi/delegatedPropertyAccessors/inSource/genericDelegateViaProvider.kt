// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

class GenericDelegate<T>(private val value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {

    }
}

class ProvideDelegateHolder(val data: Any)

operator fun <T> ProvideDelegateHolder.provideDelegate(thisRef: Any?, property: KProperty<*>): GenericDelegate<T> {
    @Suppress("UNCHECKED_CAST")
    return GenericDelegate(data as T)
}

var prop: String <expr>by ProvideDelegateHolder("provided")</expr>
