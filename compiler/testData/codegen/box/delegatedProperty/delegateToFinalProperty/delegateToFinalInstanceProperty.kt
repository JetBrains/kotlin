// WITH_STDLIB
// CHECK_BYTECODE_LISTING

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class C() {
    val impl = 123
    val s: String by impl
}

operator fun Any?.getValue(thisRef: Any?, property: KProperty<*>) = "OK"

fun box() = C().s
