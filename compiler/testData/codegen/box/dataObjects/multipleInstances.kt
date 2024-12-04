// LANGUAGE: +DataObjects
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

import kotlin.test.*

data object DataObject

val doppelganger = DataObject::class.java.declaredConstructors[0].apply { isAccessible = true }.newInstance()

fun box(): String {
    assertFalse(DataObject === doppelganger)
    assertEquals(DataObject, doppelganger)
    assertEquals(DataObject.hashCode(), DataObject::class.java.cast(doppelganger).hashCode())

    return  "OK"
}

