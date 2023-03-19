// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import java.lang.reflect.Type

@JvmInline
value class S(val value1: UInt, val value2: String)

fun S.foo(x: Int, s: S): S = this

fun compoundType(vararg types: Type) = listOf(*types).toString()

@JvmInline
value class T(val s: S) {
    fun bar(u: S): T = this
}
data class U(val s: S) {
    fun bar(u: S): U = this
}

@kotlin.ExperimentalStdlibApi
fun box(): String {
    assertEquals(listOf(Int::class.java, String::class.java), ::S.parameters.map { it.type.javaType })
    assertEquals(S::class.java, ::S.returnType.javaType)

    assertEquals(listOf(compoundType(Int::class.java, String::class.java)), ::T.parameters.map { it.type.javaType.toString() })
    assertEquals(T::class.java, ::T.returnType.javaType)

    assertEquals(listOf(compoundType(Int::class.java, String::class.java)), ::U.parameters.map { it.type.javaType.toString() })
    assertEquals(U::class.java, ::U.returnType.javaType)

    
    assertEquals(listOf(compoundType(Int::class.java, String::class.java), Int::class.java.toString(), compoundType(Int::class.java, String::class.java)), S::foo.parameters.map { it.type.javaType.toString() })
    assertEquals(S::class.java, S::foo.returnType.javaType)

    assertEquals(listOf(compoundType(Int::class.java, String::class.java), compoundType(Int::class.java, String::class.java)), T::bar.parameters.map { it.type.javaType.toString() })
    assertEquals(T::class.java, T::bar.returnType.javaType)

    assertEquals(listOf(U::class.java.toString(), compoundType(Int::class.java, String::class.java)), U::bar.parameters.map { it.type.javaType.toString() })
    assertEquals(U::class.java, U::bar.returnType.javaType)

    return "OK"
}
