// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun foo(unitParam: Unit, nullableUnitParam: Unit?): Unit {}

var bar: Unit = Unit

fun box(): String {
    assert(Unit::class.java != java.lang.Void.TYPE)

    assertEquals(Unit::class.java, ::foo.parameters[0].type.javaType)
    assertEquals(Unit::class.java, ::foo.parameters[1].type.javaType)
    assertEquals(java.lang.Void.TYPE, ::foo.returnType.javaType)

    assertEquals(Unit::class.java, ::bar.returnType.javaType)
    assertEquals(Unit::class.java, ::bar.getter.returnType.javaType)
    assertEquals(Unit::class.java, ::bar.setter.parameters.single().type.javaType)
    assertEquals(java.lang.Void.TYPE, ::bar.setter.returnType.javaType)

    return "OK"
}
