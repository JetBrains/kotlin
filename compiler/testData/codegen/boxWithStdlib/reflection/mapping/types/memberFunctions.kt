import kotlin.jvm.JvmStatic as static
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A {
    fun foo(t: Long?): Long = t!!
}

object O {
    @static fun bar(a: A): String = ""
}

fun box(): String {
    assertEquals(listOf(javaClass<A>(), javaClass<java.lang.Long>()), A::foo.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<O>(), javaClass<A>()), O::bar.parameters.map { it.type.javaType })

    assertEquals(java.lang.Long.TYPE, A::foo.returnType.javaType)
    assertEquals(javaClass<String>(), O::bar.returnType.javaType)

    return "OK"
}
