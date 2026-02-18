// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

val prop <expr>by GenericDelegate<String>("hello")</expr>

class GenericDelegate<T>(private val value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {

    }
}
