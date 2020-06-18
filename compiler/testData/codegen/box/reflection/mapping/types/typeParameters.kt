// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.TypeVariable
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class A<T : CharSequence> {
    fun foo(t: T) {}
}

fun box(): String {
    val f = A<String>::foo
    val t = f.parameters.last().type.javaType
    if (t !is TypeVariable<*>) return "Fail, t should be a type variable: $t"

    assertEquals("T", t.name)
    assertEquals(A::class.java, (t.genericDeclaration as Class<*>))

    val tp = A::class.typeParameters
    assertEquals(CharSequence::class.java, tp.single().upperBounds.single().javaType)

    return "OK"
}
