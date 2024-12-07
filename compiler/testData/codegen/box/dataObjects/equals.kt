// LANGUAGE: +DataObjects
// WITH_STDLIB

package com.example

import kotlin.test.*

data object DataObject

fun box(): String {
    assertEquals(DataObject, DataObject)
    assertFalse(DataObject == null)

    return  "OK"
}