import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class A(d: Double, s: String, parent: A?) {
    class Nested(a: A)
    inner class Inner(nested: Nested)
}

fun box(): String {
    assertEquals(listOf(java.lang.Double.TYPE, javaClass<String>(), javaClass<A>()), ::A.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<A>()), A::Nested.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<A>(), javaClass<A.Nested>()), A::Inner.parameters.map { it.type.javaType })

    assertEquals(javaClass<A>(), ::A.returnType.javaType)
    assertEquals(javaClass<A.Nested>(), A::Nested.returnType.javaType)
    assertEquals(javaClass<A.Inner>(), A::Inner.returnType.javaType)

    return "OK"
}
