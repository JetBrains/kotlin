// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

var prop <expr>by GenericDelegate<String>("hello")</expr>

class GenericDelegate<T>(private val value: T) {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {

    }
}

operator fun Int.getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
}