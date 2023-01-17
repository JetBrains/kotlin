// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

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
