// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*

object Delegate {
    operator fun getValue(thiz: My, property: KProperty<*>): String {
        if (property !is KMutableProperty<*>) return "Fail: property is not a KMutableProperty"
        property as KMutableProperty1<My, String>

        try {
            property.set(thiz, "")
            return "Fail: property.set should cause IllegalCallableAccessException"
        }
        catch (e: IllegalCallableAccessException) {
            // OK
        }

        property.accessible = true
        property.set(thiz, "")

        return "OK"
    }

    operator fun setValue(thiz: My, property: KProperty<*>, value: String) {
    }
}

class My {
    var delegate: String by Delegate
        private set
}

fun box() = My().delegate
