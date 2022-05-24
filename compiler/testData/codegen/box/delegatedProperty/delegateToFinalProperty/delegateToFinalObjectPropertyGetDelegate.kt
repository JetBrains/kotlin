// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object O {
    val impl = 123
}

operator fun Any?.getValue(thisRef: Any?, property: KProperty<*>) = "OK"

val s: String by O.impl

fun box(): String {
    assertEquals(123, O::s.getDelegate())
    return "OK"
}
