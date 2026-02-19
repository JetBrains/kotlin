// WITH_STDLIB

import kotlin.test.assertEquals

const val constTrue = true
const val const42 = 42
const val constPiF = 3.14F
const val constPi = 3.1415926358
const val constString = "string"

fun box(): String {
    assertEquals("true", "$constTrue")
    assertEquals("42", "$const42")
    assertEquals("3.14", "$constPiF")
    assertEquals("3.1415926358", "$constPi")
    assertEquals("string", "$constString")

    assertEquals(constPi.toString(), "$constPi")
    assertEquals((constPi * constPi).toString(), "${constPi * constPi}")

    assertEquals("null", "${null}")
    assertEquals("42", "${42}")

    return "OK"
}