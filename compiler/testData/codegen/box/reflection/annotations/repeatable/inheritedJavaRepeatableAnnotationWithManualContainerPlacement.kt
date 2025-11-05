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
annotation class Anno(val value: String)

@java.lang.annotation.Inherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class JAnnoContainer(val value: Array<Anno>)

@JAnnoContainer([Anno("1"), Anno("2")])
@Anno("3")
open class BaseClass

@JAnnoContainer([])
class ChildClass1: BaseClass()

@JAnnoContainer([Anno("4")])
class ChildClass2: BaseClass()

private fun testAnnotationsJavaDifference() {
    fun javaAnnotations(klass: KClass<*>) = klass.java.annotations.filter { it.annotationClass.simpleName != "Metadata" }.toSet()
    fun kotlinAnnotations(klass: KClass<*>) = klass.annotations.toSet()

    // setOf(Anno("3"), JAnnoContainer(arrayOf(Anno("1"), Anno("2"))))
    assertEquals(javaAnnotations(BaseClass::class), kotlinAnnotations(BaseClass::class))

    // Although formally the container is manually placed, it is indistinguishable from the one created for multiple annotations in most of
    // the cases. Even when it is possible, i.e. for none or one annotation in the container, we still consider that it "shadows"
    // it is a different annotation than @Anno, in fact it shodows @Anno on BaseClass! (TODO is it in Java???)
    // Java getAnnotations() misses support of shadowing between single annotations and containers

    assertEquals(
        setOf(Anno("3"), JAnnoContainer(arrayOf<Anno>())),
        javaAnnotations(ChildClass1::class))
    assertEquals(
        setOf(JAnnoContainer(arrayOf<Anno>())),
        kotlinAnnotations(ChildClass1::class))

    assertEquals(
        setOf(Anno("3"), JAnnoContainer(arrayOf(Anno("4")))),
        javaAnnotations(ChildClass2::class))
    assertEquals(
        setOf(JAnnoContainer(arrayOf(Anno("4")))),
        kotlinAnnotations(ChildClass2::class))
}

private fun testFindAnnotationsJavaDifference() {
    fun javaAnnotations(klass: KClass<*>) = klass.java.getAnnotationsByType(Anno::class.java).toSet()
    fun kotlinAnnotations(klass: KClass<*>) = klass.findAnnotations<Anno>().toSet()

    // TODO(Review): shall we fix it? It is not related to inheritance, this weird case is just not supported in findAnnotations()
    assertEquals(
        setOf(Anno("1"), Anno("2"), Anno("3")),
        javaAnnotations(BaseClass::class));
    assertEquals(
        setOf(Anno("3")),
        kotlinAnnotations(BaseClass::class));

    // TODO(Review): shall we fix it? It seems that Java's implementation do not consider overwriting with empty container as "shadowing"
    assertEquals(
        setOf(Anno("1"), Anno("2"), Anno("3")),
        javaAnnotations(ChildClass1::class));
    assertEquals(
        setOf<Anno>(),
        kotlinAnnotations(ChildClass1::class));

    assertEquals(javaAnnotations(ChildClass2::class), kotlinAnnotations(ChildClass2::class))
}

fun box(): String {
    testAnnotationsJavaDifference()
    testFindAnnotationsJavaDifference()

    return "OK"
}
