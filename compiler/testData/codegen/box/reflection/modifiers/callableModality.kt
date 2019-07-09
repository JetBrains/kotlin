// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

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

fun box(): String {
    assertFalse(Interface::openFun.isFinal)
    assertTrue(Interface::openFun.isOpen)
    assertFalse(Interface::openFun.isAbstract)

    assertFalse(Interface::abstractFun.isFinal)
    assertFalse(Interface::abstractFun.isOpen)
    assertTrue(Interface::abstractFun.isAbstract)

    assertTrue(AbstractClass::finalVal.isFinal)
    assertFalse(AbstractClass::finalVal.isOpen)
    assertFalse(AbstractClass::finalVal.isAbstract)
    assertTrue(AbstractClass::finalVal.getter.isFinal)
    assertFalse(AbstractClass::finalVal.getter.isOpen)
    assertFalse(AbstractClass::finalVal.getter.isAbstract)

    assertFalse(AbstractClass::openVal.isFinal)
    assertTrue(AbstractClass::openVal.isOpen)
    assertFalse(AbstractClass::openVal.isAbstract)
    assertFalse(AbstractClass::openVal.getter.isFinal)
    assertTrue(AbstractClass::openVal.getter.isOpen)
    assertFalse(AbstractClass::openVal.getter.isAbstract)

    assertFalse(AbstractClass::abstractVar.isFinal)
    assertFalse(AbstractClass::abstractVar.isOpen)
    assertTrue(AbstractClass::abstractVar.isAbstract)
    assertFalse(AbstractClass::abstractVar.getter.isFinal)
    assertFalse(AbstractClass::abstractVar.getter.isOpen)
    assertTrue(AbstractClass::abstractVar.getter.isAbstract)
    assertFalse(AbstractClass::abstractVar.setter.isFinal)
    assertFalse(AbstractClass::abstractVar.setter.isOpen)
    assertTrue(AbstractClass::abstractVar.setter.isAbstract)

    return "OK"
}
