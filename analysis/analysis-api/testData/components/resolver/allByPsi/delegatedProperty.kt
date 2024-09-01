// WITH_REFLECT
package myPack

import kotlin.reflect.KProperty

var prop by Delegate()

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return ""
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}