// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.coroutines.SuspendFunction3
import kotlin.reflect.KClass
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

private fun checkFinal(klass: KClass<*>) {
    assertTrue(klass.isFinal)
    assertFalse(klass.isOpen)
    assertFalse(klass.isAbstract)
}

private fun checkOpen(klass: KClass<*>) {
    assertFalse(klass.isFinal)
    assertTrue(klass.isOpen)
    assertFalse(klass.isAbstract)
}

private fun checkAbstract(klass: KClass<*>) {
    assertFalse(klass.isFinal)
    assertFalse(klass.isOpen)
    assertTrue(klass.isAbstract)
}

fun box(): String {
    checkFinal(FinalClass::class)
    checkFinal(FinalClass.Companion::class)
    checkOpen(OpenClass::class)
    checkAbstract(AbstractClass::class)
    checkAbstract(Interface::class)
    checkFinal(EnumClass::class)
    checkFinal(EnumClassWithAbstractMember::class)
    // Note that unlike in JVM, annotation classes are final in Kotlin
    checkFinal(AnnotationClass::class)
    checkFinal(Object::class)

    checkAbstract(Function0::class)
    checkAbstract(SuspendFunction3::class)

    return "OK"
}
