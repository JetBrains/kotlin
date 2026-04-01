// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun string(): String = ""
fun stringN(): String? = ""
fun int(): Int = 0
fun intN(): Int? = 0

fun checkObjectType(nullable: KType, notNull: KType, klass: KClass<*>) {
    assertEquals(klass.java, notNull.javaType)
    assertEquals(klass.java, nullable.javaType)
    assertEquals(nullable.javaType, nullable.withNullability(false).javaType)
    assertEquals(nullable.javaType, nullable.withNullability(true).javaType)
    assertEquals(notNull.javaType, notNull.withNullability(false).javaType)
    assertEquals(nullable.javaType, notNull.withNullability(true).javaType)
}

fun checkPrimitiveType(nullable: KType, notNull: KType, klass: KClass<*>) {
    assertEquals(klass.javaPrimitiveType!!, notNull.javaType)
    assertEquals(klass.javaObjectType, nullable.javaType)
    assertEquals(notNull.javaType, nullable.withNullability(false).javaType)
    assertEquals(nullable.javaType, nullable.withNullability(true).javaType)
    assertEquals(notNull.javaType, notNull.withNullability(false).javaType)
    assertEquals(nullable.javaType, notNull.withNullability(true).javaType)
}

fun box(): String {
    checkObjectType(::stringN.returnType, ::string.returnType, String::class)
    checkObjectType(typeOf<String?>(), typeOf<String>(), String::class)

    checkPrimitiveType(::intN.returnType, ::int.returnType, Int::class)
    checkPrimitiveType(typeOf<Int?>(), typeOf<Int>(), Int::class)

    return "OK"
}
