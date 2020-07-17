// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public interface J {
    String platform();
}

// FILE: K.kt

import kotlin.reflect.full.withNullability
import kotlin.test.assertEquals

fun nonNull(): String = ""
fun nullable(): String? = ""

fun box(): String {
    val nonNull = ::nonNull.returnType
    val nullable = ::nullable.returnType
    val platform = J::platform.returnType

    assertEquals(nonNull, nullable.withNullability(false))
    assertEquals(nullable, nullable.withNullability(true))
    assertEquals(nonNull, nonNull.withNullability(false))
    assertEquals(nullable, nonNull.withNullability(true))

    assertEquals(nonNull, platform.withNullability(false))
    assertEquals(nullable, platform.withNullability(true))

    return "OK"
}
