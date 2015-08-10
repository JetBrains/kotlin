import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

fun foo(unitParam: Unit, nullableUnitParam: Unit?): Unit {}

var bar: Unit = Unit

fun box(): String {
    assert(javaClass<Unit>() != java.lang.Void.TYPE)

    assertEquals(javaClass<Unit>(), ::foo.parameters[0].type.javaType)
    assertEquals(javaClass<Unit>(), ::foo.parameters[1].type.javaType)
    assertEquals(java.lang.Void.TYPE, ::foo.returnType.javaType)

    assertEquals(javaClass<Unit>(), ::bar.returnType.javaType)
    assertEquals(javaClass<Unit>(), ::bar.getter.returnType.javaType)
    assertEquals(javaClass<Unit>(), ::bar.setter.parameters.single().type.javaType)
    assertEquals(java.lang.Void.TYPE, ::bar.setter.returnType.javaType)

    return "OK"
}
