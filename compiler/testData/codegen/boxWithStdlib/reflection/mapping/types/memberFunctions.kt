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
    val foo = A::foo
    assertEquals(listOf(javaClass<A>(), javaClass<java.lang.Long>()), foo.parameters.map { it.type.javaType })
    assertEquals(java.lang.Long.TYPE, foo.returnType.javaType)

    val bar = O::class.members.single { it.name == "bar" }
    assertEquals(listOf(javaClass<O>(), javaClass<A>()), bar.parameters.map { it.type.javaType })
    assertEquals(javaClass<String>(), bar.returnType.javaType)

    return "OK"
}
