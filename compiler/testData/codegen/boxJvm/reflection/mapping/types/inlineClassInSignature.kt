// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.reflect.KCallable
import kotlin.test.assertEquals
import java.lang.reflect.Type

@JvmInline
value class S(val value: String)

fun S.foo(x: Int, s: S): S = this

@JvmInline
value class T(val s: S) {
    fun bar(u: S): T = this
}

var Int.baz: S
    get() = S(toString())
    set(value) {}

private val KCallable<*>.javaParameterTypes: List<Type>
    get() = parameters.map { it.type.javaType }

fun box(): String {
    val s = String::class.java
    val int = Int::class.java
    
    assertEquals(listOf(s, int, s), S::foo.javaParameterTypes)
    assertEquals(s, S::foo.returnType.javaType)

    assertEquals(listOf(s, s), T::bar.javaParameterTypes)
    assertEquals(s, T::bar.returnType.javaType)

    assertEquals(listOf(int), Int::baz.javaParameterTypes)
    assertEquals(s, Int::baz.returnType.javaType)
    assertEquals(listOf(int), Int::baz.getter.javaParameterTypes)
    assertEquals(s, Int::baz.getter.returnType.javaType)
    assertEquals(listOf(int, s), Int::baz.setter.javaParameterTypes)
    assertEquals(Void.TYPE, Int::baz.setter.returnType.javaType)

    return "OK"
}
