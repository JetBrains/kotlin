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
import kotlin.test.assertTrue

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotInheritedAnno

@java.lang.annotation.Inherited
@Repeatable
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(val value: String = "myDefaultValue")

@NotInheritedAnno
@Anno
open class BaseClass

@Anno("1")
@Anno("2")
open class MiddleClass: BaseClass()

@Anno("3")
class ChildClass1: MiddleClass()

class ChildClass2: MiddleClass()

private fun test(klass: KClass<*>, vararg expectedValues: String) {
    val expected = expectedValues.map { Anno(it) }.toSet()

    assertEquals(expected, klass.annotations.toSet(), "Failed annotations for $klass")
    assertEquals(expected, klass.findAnnotations<Anno>().toSet(), "Failed findAnnotations() for $klass")

    val found = klass.findAnnotation<Anno>() // any of expected annotations may be returned
    assertNotNull(found, "Failed findAnnotation() for $klass: not found")
    assertContains(expected, found, "Failed findAnnotation() for $klass: invalid result")

    assertTrue(klass.hasAnnotation<Anno>(), "Failed hasAnnotation for $klass")
}

fun box(): String {
    test(MiddleClass::class, "1", "2")
    test(ChildClass1::class, "3")
    test(ChildClass2::class, "1", "2")

    return "OK"
}
