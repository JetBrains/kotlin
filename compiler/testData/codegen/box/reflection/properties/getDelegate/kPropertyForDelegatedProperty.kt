// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.test.*

var ref: KProperty<*>? = null

class Delegate {
    var storage = ""
    operator fun provideDelegate(instance: Any?, property: KProperty<*>): Delegate {
        ref = property
        return this
    }
    operator fun getValue(instance: Any?, property: KProperty<*>): String = storage
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) { storage = value }
}

var result: String by Delegate()

fun box(): String {
    result
    val prop = ref as KProperty0<*>

    result = "Fail"
    val d = prop.apply { isAccessible = true }.getDelegate() as Delegate
    result = "OK"
    assertEquals(d, prop.apply { isAccessible = true }.getDelegate())
    return result
}
