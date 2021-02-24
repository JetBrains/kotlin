// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun nonNull(): String = ""
fun nullable(): String? = ""

fun box(): String {
    val nonNull = ::nonNull.returnType
    val nullable = ::nullable.returnType

    assertEquals(nullable.javaType, nullable.withNullability(false).javaType)
    assertEquals(nullable.javaType, nullable.withNullability(true).javaType)
    assertEquals(nonNull.javaType, nonNull.withNullability(false).javaType)
    assertEquals(nullable.javaType, nonNull.withNullability(true).javaType)

    return "OK"
}
