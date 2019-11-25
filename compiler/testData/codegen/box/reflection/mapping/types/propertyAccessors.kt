// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A(private var foo: String)

object O {
    @JvmStatic
    private var bar: String = ""
}

fun box(): String {
    val foo = A::class.members.single { it.name == "foo" } as KMutableProperty<*>
    assertEquals(listOf(A::class.java), foo.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java), foo.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, String::class.java), foo.setter.parameters.map { it.type.javaType })

    val bar = O::class.members.single { it.name == "bar" } as KMutableProperty<*>
    assertEquals(listOf(O::class.java), bar.parameters.map { it.type.javaType })
    assertEquals(listOf(O::class.java), bar.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(O::class.java, String::class.java), bar.setter.parameters.map { it.type.javaType })

    return "OK"
}
