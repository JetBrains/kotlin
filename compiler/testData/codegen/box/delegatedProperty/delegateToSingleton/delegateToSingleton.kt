// WITH_STDLIB
// WITH_REFLECT

import kotlin.reflect.KProperty

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
    assert(O::s.getDelegate() == Store)
    O.s = "OK"
    return O.s
}