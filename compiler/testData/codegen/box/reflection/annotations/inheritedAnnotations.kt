// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.declaredFunctions

import kotlin.test.assertEquals
import kotlin.test.fail

@java.lang.annotation.Inherited
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(val value: String = "myDefaultValue")

@Anno
open class BaseClass {
    @Anno("Base::foo")
    open fun foo() = "base"
}

@Anno("OK")
open class MiddleClass: BaseClass() {
    override fun foo() = "middle"
}

class ChildClass: MiddleClass()

private fun testAnnotations() {
    fun <T: Any> getAnnoValue(klass: KClass<T>) =
        (klass.annotations.single() as Anno).value

    assertEquals("myDefaultValue", getAnnoValue(BaseClass::class))
    assertEquals("OK", getAnnoValue(MiddleClass::class))
    assertEquals("OK", getAnnoValue(ChildClass::class))
}

private fun testFindAnnotation() {
    fun <T: Any> getAnnoValue(klass: KClass<T>) =
        (klass.findAnnotation<Anno>() ?: fail("findAnnotation failed for $klass")).value

    assertEquals("myDefaultValue", getAnnoValue(BaseClass::class))
    assertEquals("OK", getAnnoValue(MiddleClass::class))
    assertEquals("OK", getAnnoValue(ChildClass::class))
}

private fun testFindAnnotations() {
    fun <T: Any> getAnnoValue(klass: KClass<T>) =
        klass.findAnnotations<Anno>().single().value

    assertEquals("myDefaultValue", getAnnoValue(BaseClass::class))
    assertEquals("OK", getAnnoValue(MiddleClass::class))
    assertEquals("OK", getAnnoValue(ChildClass::class))
}

private fun testJavaGetAnnotationsByType() {
    fun <T: Any> getAnnoContent(klass: KClass<T>) =
        klass.java.getAnnotationsByType(Anno::class.java).contentToString()

    assertEquals("[@Anno(value=myDefaultValue)]", getAnnoContent(BaseClass::class))
    assertEquals("[@Anno(value=OK)]", getAnnoContent(MiddleClass::class))
    assertEquals("[@Anno(value=OK)]", getAnnoContent(ChildClass::class))
}

private fun testNoInheritanceOnFunctions() {
    val foo = MiddleClass::class.declaredFunctions.filter { it.name == "foo" }.single()
    assertEquals(0, foo.annotations.size)
}

@Anno
interface I1

interface I2: I1

class C: I1

private fun testNoInheritanceFromInterfaces() {
    assertEquals(0, I2::class.annotations.size)
    assertEquals(0, C::class.annotations.size)
}

fun box(): String {
    testAnnotations()
    testFindAnnotation()
    testFindAnnotations()
    testJavaGetAnnotationsByType()
    testNoInheritanceOnFunctions()
    testNoInheritanceFromInterfaces()

    return "OK"
}
