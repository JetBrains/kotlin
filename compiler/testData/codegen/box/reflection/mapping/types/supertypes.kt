// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

open class Klass
interface Interface<T, U>
interface Interface2

class A<Z> : Interface<String, Z>, Klass(), Interface2

fun box(): String {
    val (i, k, i2) = A::class.supertypes.map { it.javaType }

    i as? ParameterizedType ?: fail("Not a parameterized type: $i")
    assertEquals(Interface::class.java, i.rawType)
    val args = i.actualTypeArguments
    assertEquals(String::class.java, args[0], "Not String: ${args[0]}")
    assertTrue(args[1].let { it is TypeVariable<*> && it.name == "Z" && it.genericDeclaration == A::class.java }, "Not Z: ${args[1]}")

    assertEquals(Klass::class.java, k)

    assertEquals(Interface2::class.java, i2)

    return "OK"
}
