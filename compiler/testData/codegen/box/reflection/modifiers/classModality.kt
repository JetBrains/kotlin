// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertTrue
import kotlin.test.assertFalse

class FinalClass {
    companion object Companion
}
open class OpenClass
abstract class AbstractClass
interface Interface
enum class EnumClass
enum class EnumClassWithAbstractMember { ; abstract fun foo() }
annotation class AnnotationClass
object Object

fun box(): String {
    assertTrue(FinalClass::class.isFinal)
    assertFalse(FinalClass::class.isOpen)
    assertFalse(FinalClass::class.isAbstract)

    assertTrue(FinalClass.Companion::class.isFinal)
    assertFalse(FinalClass.Companion::class.isOpen)
    assertFalse(FinalClass.Companion::class.isAbstract)

    assertFalse(OpenClass::class.isFinal)
    assertTrue(OpenClass::class.isOpen)
    assertFalse(OpenClass::class.isAbstract)

    assertFalse(AbstractClass::class.isFinal)
    assertFalse(AbstractClass::class.isOpen)
    assertTrue(AbstractClass::class.isAbstract)

    assertFalse(Interface::class.isFinal)
    assertFalse(Interface::class.isOpen)
    assertTrue(Interface::class.isAbstract)

    assertTrue(EnumClass::class.isFinal)
    assertFalse(EnumClass::class.isOpen)
    assertFalse(EnumClass::class.isAbstract)

    assertTrue(EnumClassWithAbstractMember::class.isFinal)
    assertFalse(EnumClassWithAbstractMember::class.isOpen)
    assertFalse(EnumClassWithAbstractMember::class.isAbstract)

    // Note that unlike in JVM, annotation classes are final in Kotlin
    assertTrue(AnnotationClass::class.isFinal)
    assertFalse(AnnotationClass::class.isOpen)
    assertFalse(AnnotationClass::class.isAbstract)

    assertTrue(Object::class.isFinal)
    assertFalse(Object::class.isOpen)
    assertFalse(Object::class.isAbstract)

    return "OK"
}
