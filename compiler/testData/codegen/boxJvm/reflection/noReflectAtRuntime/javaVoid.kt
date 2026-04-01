// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KClass
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(Void::class, Void.TYPE.kotlin)
    assertEquals(Void.TYPE.kotlin, Void::class)

    assertEquals(Void.TYPE, Void::class.javaPrimitiveType)
    assertEquals(Void::class.java, Void::class.javaObjectType)
    assertEquals(Void.TYPE, Void.TYPE.kotlin.javaPrimitiveType)
    assertEquals(Void::class.java, Void.TYPE.kotlin.javaObjectType)

    assertEquals("Void", Void::class.simpleName)
    assertEquals("java.lang.Void", Void::class.qualifiedName)

    return "OK"
}
