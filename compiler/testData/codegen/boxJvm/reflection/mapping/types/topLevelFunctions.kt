// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun free(s: String): Int = s.length

fun Any.extension() {}

fun box(): String {
    assertEquals(java.lang.Integer.TYPE, ::free.returnType.javaType)
    assertEquals(String::class.java, ::free.parameters.single().type.javaType)

    assertEquals(Any::class.java, Any::extension.parameters.single().type.javaType)

    return "OK"
}
