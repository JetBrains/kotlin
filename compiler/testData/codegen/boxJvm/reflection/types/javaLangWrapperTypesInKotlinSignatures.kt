// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    public static void java(Character c, char p) {}
}

// FILE: box.kt
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

import kotlin.reflect.KCallable
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertEquals

// Explicit usages of java.lang wrapper types in Kotlin signatures must be distinguishable from primitives via
// `jvmErasure.java`/`javaType`, consistently for top-level and member functions, and with Java declarations (KT-85550, KT-88884).

interface Foo {
    fun member(c: Character, p: Char, n: Character?)
}

fun topLevel(c: Character, p: Char, n: Character?) {}

private fun KCallable<*>.erasures(): List<Class<*>> = valueParameters.map { it.type.jvmErasure.java }
private fun KCallable<*>.javaTypes(): List<java.lang.reflect.Type> = valueParameters.map { it.type.javaType }

private val boxed = Character::class.java
private val primitive = Char::class.javaPrimitiveType!!

fun box(): String {
    assertEquals(listOf(boxed, primitive), J::java.erasures())
    assertEquals(listOf(boxed, primitive), J::java.javaTypes())

    assertEquals(listOf(boxed, primitive, boxed), ::topLevel.javaTypes())
    assertEquals(listOf(boxed, primitive, boxed), Foo::member.javaTypes())

    // TODO(KT-88884): jvmErasure of a non-null `Character` parameter is primitive.
    assertEquals(listOf(primitive, primitive, boxed), ::topLevel.erasures())
    assertEquals(listOf(primitive, primitive, boxed), Foo::member.erasures())

    return "OK"
}
