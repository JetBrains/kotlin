// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A(private var foo: String) {
    var String.memExt: Int
        get() = 0
        set(value) {}
}

object O {
    @JvmStatic
    private var bar: String = ""
}

var String.ext: Int
    get() = 0
    set(value) {}

fun box(): String {
    val foo = A::class.members.single { it.name == "foo" } as KMutableProperty<*>
    assertEquals(listOf(A::class.java), foo.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java), foo.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, String::class.java), foo.setter.parameters.map { it.type.javaType })

    val bar = O::class.members.single { it.name == "bar" } as KMutableProperty<*>
    assertEquals(listOf(O::class.java), bar.parameters.map { it.type.javaType })
    assertEquals(listOf(O::class.java), bar.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(O::class.java, String::class.java), bar.setter.parameters.map { it.type.javaType })

    val memExt = A::class.members.single { it.name == "memExt" } as KMutableProperty<*>
    assertEquals(listOf(A::class.java, String::class.java), memExt.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, String::class.java), memExt.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, String::class.java, Int::class.java), memExt.setter.parameters.map { it.type.javaType })

    val ext = String::ext
    assertEquals(listOf(String::class.java), ext.parameters.map { it.type.javaType })
    assertEquals(listOf(String::class.java), ext.getter.parameters.map { it.type.javaType })
    assertEquals(listOf(String::class.java, Int::class.java), ext.setter.parameters.map { it.type.javaType })

    return "OK"
}
