// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

class GenericDelegate<T>(private val value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
}

class ProvideDelegateHolder(val data: Any)

operator fun <T> ProvideDelegateHolder.provideDelegate(thisRef: Any?, property: KProperty<*>): GenericDelegate<T> {
    return GenericDelegate(null!!)
}

var prop: String <expr>by ProvideDelegateHolder("provided")</expr>
