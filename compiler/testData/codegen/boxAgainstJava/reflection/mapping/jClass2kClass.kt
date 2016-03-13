// FILE: J.java

public class J {}

// FILE: 1.kt

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun box(): String {
    val j = J::class.java
    assertEquals(j, j.kotlin.java)

    return "OK"
}
