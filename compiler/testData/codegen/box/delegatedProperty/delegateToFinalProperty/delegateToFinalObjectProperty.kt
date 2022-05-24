// WITH_STDLIB
// CHECK_BYTECODE_LISTING

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object O {
    val impl = 123
}

operator fun Any?.getValue(thisRef: Any?, property: KProperty<*>) = "OK"

val s: String by O.impl

fun box() = s
