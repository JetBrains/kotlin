// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.3.0
// KT-85411: Supported in 2.4.0-Beta2
// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

import kotlin.reflect.KProperty
import kotlin.test.assertEquals

object Store {
    private val map = mutableMapOf<Pair<Any?, KProperty<*>>, String?>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = map[thisRef to property]

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        map[thisRef to property] = value
    }
}

object O {
    var s: String? by Store
}

fun box(): String? {
    O.s = "OK"
    return O.s
}
