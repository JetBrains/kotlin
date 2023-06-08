// ISSUE: KT-35707

// Light analysis mode test is muted because of some bug related to the old JVM backend. To be unmuted once the test is migrated to JVM IR.
// IGNORE_LIGHT_ANALYSIS

import kotlin.reflect.KProperty

interface PropertyDelegate {
    operator fun getValue(thisRef: A, property: KProperty<*>): Boolean = true
    operator fun setValue(thisRef: A, property: KProperty<*>, value: Boolean) {}
}
class A {
    val b by object : PropertyDelegate {}
}

fun box(): String {
    return if (A().b) {
        "OK"
    } else {
        "FAIL"
    }
}
