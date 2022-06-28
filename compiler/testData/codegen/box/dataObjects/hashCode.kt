// LANGUAGE: +DataObjects
// WITH_STDLIB

package com.example

import kotlin.test.*

data object DataObject {
    data object Inner
}

fun box(): String {
    assertEquals(DataObject.hashCode(), DataObject.hashCode())
    assertNotEquals(DataObject.hashCode(), DataObject.Inner.hashCode())
    assertNotEquals(0, DataObject.hashCode())
    assertNotEquals(0, DataObject.Inner.hashCode())

    return "OK"
}
