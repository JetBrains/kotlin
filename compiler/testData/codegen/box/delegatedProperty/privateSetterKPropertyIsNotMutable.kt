// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

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

        property.isAccessible = true
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
