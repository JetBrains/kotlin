// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.test.assertTrue
import kotlin.test.assertFalse

interface Interface {
    open fun openFun() {}
    abstract fun abstractFun()
}

abstract class AbstractClass {
    final val finalVal = Unit
    open val openVal = Unit
    abstract var abstractVar: Unit
}

class Constructor

private fun checkFinal(callable: KCallable<*>) {
    assertTrue(callable.isFinal)
    assertFalse(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkOpen(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertTrue(callable.isOpen)
    assertFalse(callable.isAbstract)
}

private fun checkAbstract(callable: KCallable<*>) {
    assertFalse(callable.isFinal)
    assertFalse(callable.isOpen)
    assertTrue(callable.isAbstract)
}

fun box(): String {
    checkOpen(Interface::openFun)
    checkAbstract(Interface::abstractFun)

    checkFinal(AbstractClass::finalVal)
    checkFinal(AbstractClass::finalVal.getter)
    checkOpen(AbstractClass::openVal)
    checkOpen(AbstractClass::openVal.getter)
    checkAbstract(AbstractClass::abstractVar)
    checkAbstract(AbstractClass::abstractVar.getter)
    checkAbstract(AbstractClass::abstractVar.setter)

    checkFinal(::Constructor)

    return "OK"
}
