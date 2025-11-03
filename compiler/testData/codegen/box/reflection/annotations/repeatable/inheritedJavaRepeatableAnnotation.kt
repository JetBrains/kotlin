// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

package test

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@java.lang.annotation.Inherited
@java.lang.annotation.Repeatable(JAnnoContainer::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(val value: String = "myDefaultValue")

@java.lang.annotation.Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JAnnoContainer(val value: Array<Anno>)

@Anno
open class BaseClass

@Anno("1")
@Anno("2")
open class MiddleClass: BaseClass()

@Anno("3")
class ChildClass1: MiddleClass()

class ChildClass2: MiddleClass()

private fun test(klass: KClass<*>, expectedContainer: Boolean, vararg expectedValues: String) {
    val expectedUnwrapped = expectedValues.map { Anno(it) }.toSet()
    val expected = if (expectedContainer) {
        setOf(JAnnoContainer(expectedValues.map { Anno(it) }.toTypedArray()))
    } else {
        expectedUnwrapped
    }

    assertEquals(expected, klass.annotations.toSet(), "Failed annotations for $klass")
    assertEquals(expectedUnwrapped, klass.findAnnotations<Anno>().toSet(), "Failed findAnnotations() for $klass")

    if (expectedContainer) {
        val found = klass.findAnnotation<JAnnoContainer>()
        assertNotNull(found, "Failed findAnnotation<JAnnoContainer>() for $klass: not found")
        assertContains(expected, found, "Failed findAnnotation<JAnnoContainer>() for $klass: invalid result")
        assertNull(klass.findAnnotation<Anno>(), "Failed findAnnotation<Anno>() for $klass: expected null")

        assertFalse(klass.hasAnnotation<Anno>(), "Failed hasAnnotation<Anno>() for $klass")
        assertTrue(klass.hasAnnotation<JAnnoContainer>(), "Failed hasAnnotation<JAnnoContainer>() for $klass")
    } else {
        val found = klass.findAnnotation<Anno>()
        assertNotNull(found, "Failed findAnnotation<JAnno>() for $klass: not found")
        assertContains(expected, found, "Failed findAnnotation<JAnno>() for $klass: invalid result")
        assertNull(klass.findAnnotation<JAnnoContainer>(), "Failed findAnnotation<JAnno>() for $klass: expected null")

        assertTrue(klass.hasAnnotation<Anno>(), "Failed hasAnnotation<Anno>() for $klass")
        assertFalse(klass.hasAnnotation<JAnnoContainer>(), "Failed hasAnnotation<JAnnoContainer>() for $klass")
    }
}

private fun testAnnotationsJavaDifference() {
    fun javaAnnotations(klass: KClass<*>) = klass.java.annotations.filter { it.annotationClass.simpleName != "Metadata" }.toSet()
    fun kotlinAnnotations(klass: KClass<*>) = klass.annotations.toSet()

    // all differences below are because Java getAnnotations() misses shadowing between single annotations and containers

    assertEquals(
        setOf(Anno("myDefaultValue"), JAnnoContainer(arrayOf(Anno("1"), Anno("2")))),
        javaAnnotations(MiddleClass::class))
    assertEquals(
        setOf(JAnnoContainer(arrayOf(Anno("1"), Anno("2")))),
        kotlinAnnotations(MiddleClass::class))

    assertEquals(
        setOf(Anno("3"), JAnnoContainer(arrayOf(Anno("1"), Anno("2")))),
        javaAnnotations(ChildClass1::class))
    assertEquals(
        setOf(Anno("3")),
        kotlinAnnotations(ChildClass1::class))

    assertEquals(
        setOf(Anno("myDefaultValue"), JAnnoContainer(arrayOf(Anno("1"), Anno("2")))),
        javaAnnotations(ChildClass2::class))
    assertEquals(
        setOf(JAnnoContainer(arrayOf(Anno("1"), Anno("2")))),
        kotlinAnnotations(ChildClass2::class))
}

private fun testFindAnnotationsJavaDifference() {
    fun javaAnnotations(klass: KClass<*>) = klass.java.getAnnotationsByType(Anno::class.java).toSet()
    fun kotlinAnnotations(klass: KClass<*>) = klass.findAnnotations<Anno>().toSet()

    assertEquals(javaAnnotations(MiddleClass::class), kotlinAnnotations(MiddleClass::class))
    assertEquals(javaAnnotations(ChildClass1::class), kotlinAnnotations(ChildClass1::class))
    assertEquals(javaAnnotations(ChildClass2::class), kotlinAnnotations(ChildClass2::class))
}

fun box(): String {
    test(MiddleClass::class, true, "1", "2")
    test(ChildClass1::class, false, "3")
    test(ChildClass2::class, true, "1", "2")

    testAnnotationsJavaDifference()
    testFindAnnotationsJavaDifference()

    return "OK"
}
