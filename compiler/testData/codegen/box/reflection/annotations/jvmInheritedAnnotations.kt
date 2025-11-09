// TARGET_BACKEND: JVM
// API_VERSION: 2.3
// WITH_REFLECT
// FULL_JDK

package test

import kotlin.jvm.JvmInherited
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.declaredFunctions

import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@JvmInherited
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno(val value: String = "myDefaultValue")

@Anno
open class BaseClass

class ChildClass: BaseClass()

fun box(): String {
    assertNotNull(Anno::class.java.getAnnotation(java.lang.annotation.Inherited::class.java))
    assertTrue(ChildClass::class.annotations.single() is Anno)

    return "OK"
}
