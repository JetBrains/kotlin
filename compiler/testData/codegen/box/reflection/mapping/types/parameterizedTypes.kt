// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.ParameterizedType
import kotlin.reflect.*
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class A(private var foo: List<String>)

object O {
    @JvmStatic
    private var bar: List<String> = listOf()
}

fun topLevel(): List<String> = listOf()
fun Any.extension(): List<String> = listOf()

fun assertGenericType(type: KType) {
    val javaType = type.javaType
    if (javaType !is ParameterizedType) {
        throw AssertionError("Type should be a parameterized type, but was $javaType (${javaType.javaClass})")
    }
}

fun box(): String {
    val foo = A::class.members.single { it.name == "foo" } as KMutableProperty<*>
    assertGenericType(foo.returnType)
    assertGenericType(foo.getter.returnType)
    assertGenericType(foo.setter.parameters.last().type)

    val bar = O::class.members.single { it.name == "bar" } as KMutableProperty<*>
    assertGenericType(bar.returnType)
    assertGenericType(bar.getter.returnType)
    assertGenericType(bar.setter.parameters.last().type)

    assertGenericType(::topLevel.returnType)
    assertGenericType(Any::extension.returnType)
    assertGenericType(::A.parameters.single().type)

    return "OK"
}
