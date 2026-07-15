// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.reflect.KProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Delegate(var storage: String) {
    operator fun getValue(instance: Any?, property: KProperty<*>) = storage
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) { storage = value }
}

class A {
    companion {
        var p: String by Delegate("p")
    }
}

fun box(): String {
    assertEquals("p", A::p.call())
    assertEquals(Unit, A::p.setter.call("a"))
    assertEquals("a", A::p.call())
    assertTrue(A::p is KMutableProperty0<*>)

    val pd = (A::p).apply { isAccessible = true }.getDelegate() as Delegate
    assertEquals("a", pd.storage)

    return "OK"
}
