import kotlin.jvm.JvmStatic as static
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A(private var foo: String)

object O {
    private @static var bar: String = ""
}

fun box(): String {
    val foo = A::class.members.single { it.name == "foo" } as KMutableProperty<*>
    assertEquals(listOf(javaClass<A>()), foo.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<A>()), foo.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<A>(), javaClass<String>()), foo.setter.parameters.map { it.type.javaType })

    val bar = O::class.members.single { it.name == "bar" } as KMutableProperty<*>
    assertEquals(listOf(javaClass<O>()), bar.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<O>()), bar.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(javaClass<O>(), javaClass<String>()), bar.setter.parameters.map { it.type.javaType })

    return "OK"
}
