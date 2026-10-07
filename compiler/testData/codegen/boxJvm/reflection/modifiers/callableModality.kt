// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: JConstructor.java

public class JConstructor {}

// FILE: box.kt

// Modality of Kotlin callables. See javaCallableModality.kt for Java callables and callableModifiersInMixedHierarchies.kt for
// Kotlin callables inherited by Java classes and vice versa.

import kotlin.reflect.KClass
import kotlin.reflect.KCallable
import kotlin.test.assertEquals
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

open class OpenBase {
    open fun openFun() = "OpenBase.openFun"
    fun finalFun() = "OpenBase.finalFun"
}

abstract class AbstractBase {
    abstract fun abstractFun(): String
}

open class FinalOverride : OpenBase() {
    final override fun openFun() = "FinalOverride.openFun"
}

class OverrideInFinalClass : OpenBase() {
    override fun openFun() = "OverrideInFinalClass.openFun"
}

abstract class AbstractOverride : OpenBase() {
    abstract override fun openFun(): String
}

class AbstractOverrideImpl : AbstractOverride() {
    override fun openFun() = "AbstractOverrideImpl.openFun"
}

abstract class FakeOverridesInAbstractClass : AbstractBase()

class FakeOverridesInAbstractClassImpl : FakeOverridesInAbstractClass() {
    override fun abstractFun() = "FakeOverridesInAbstractClassImpl.abstractFun"
}

class FakeOverridesInFinalClass : OpenBase()

interface BaseInterface {
    fun abstractFun(): String
    fun defaultFun() = "BaseInterface.defaultFun"
    private fun privateFun() = "BaseInterface.privateFun"
}

interface FakeOverridesInSubinterface : BaseInterface

interface OverrideInSubinterface : BaseInterface {
    override fun defaultFun() = "OverrideInSubinterface.defaultFun"
}

interface AbstractOverrideInSubinterface : BaseInterface {
    override fun defaultFun(): String
}

class FakeOverridesInSubinterfaceImpl : FakeOverridesInSubinterface {
    override fun abstractFun() = "FakeOverridesInSubinterfaceImpl.abstractFun"
}

class OverrideInSubinterfaceImpl : OverrideInSubinterface {
    override fun abstractFun() = "OverrideInSubinterfaceImpl.abstractFun"
}

class AbstractOverrideInSubinterfaceImpl : AbstractOverrideInSubinterface {
    override fun abstractFun() = "AbstractOverrideInSubinterfaceImpl.abstractFun"
    override fun defaultFun() = "AbstractOverrideInSubinterfaceImpl.defaultFun"
}

private fun KClass<*>.member(name: String): KCallable<*> = members.single { it.name == name }

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
    checkFinal(::JConstructor)

    // Overrides.
    FinalOverride::class.member("openFun").let {
        checkFinal(it)
        assertEquals("FinalOverride.openFun", it.call(FinalOverride()))
    }
    // An override is open unless it is explicitly final, even if the containing class is final.
    OverrideInFinalClass::class.member("openFun").let {
        checkOpen(it)
        assertEquals("OverrideInFinalClass.openFun", it.call(OverrideInFinalClass()))
    }
    AbstractOverride::class.member("openFun").let {
        checkAbstract(it)
        assertEquals("AbstractOverrideImpl.openFun", it.call(AbstractOverrideImpl()))
    }

    // Fake overrides keep the modality of the inherited member.
    FakeOverridesInAbstractClass::class.member("abstractFun").let {
        checkAbstract(it)
        assertEquals("FakeOverridesInAbstractClassImpl.abstractFun", it.call(FakeOverridesInAbstractClassImpl()))
    }
    FakeOverridesInFinalClass::class.member("openFun").let {
        checkOpen(it)
        assertEquals("OpenBase.openFun", it.call(FakeOverridesInFinalClass()))
    }
    FakeOverridesInFinalClass::class.member("finalFun").let {
        checkFinal(it)
        assertEquals("OpenBase.finalFun", it.call(FakeOverridesInFinalClass()))
    }

    // Subinterfaces. Private interface members are not inherited.
    assertEquals(
        listOf("abstractFun", "defaultFun", "equals", "hashCode", "toString"),
        FakeOverridesInSubinterface::class.members.map { it.name }.sorted(),
    )
    FakeOverridesInSubinterface::class.member("abstractFun").let {
        checkAbstract(it)
        assertEquals("FakeOverridesInSubinterfaceImpl.abstractFun", it.call(FakeOverridesInSubinterfaceImpl()))
    }
    FakeOverridesInSubinterface::class.member("defaultFun").let {
        checkOpen(it)
        assertEquals("BaseInterface.defaultFun", it.call(FakeOverridesInSubinterfaceImpl()))
    }
    OverrideInSubinterface::class.member("defaultFun").let {
        checkOpen(it)
        assertEquals("OverrideInSubinterface.defaultFun", it.call(OverrideInSubinterfaceImpl()))
    }
    AbstractOverrideInSubinterface::class.member("defaultFun").let {
        checkAbstract(it)
        assertEquals("AbstractOverrideInSubinterfaceImpl.defaultFun", it.call(AbstractOverrideInSubinterfaceImpl()))
    }
    // The member of the base interface dispatches to the most specific implementation.
    assertEquals("AbstractOverrideInSubinterfaceImpl.defaultFun", BaseInterface::class.member("defaultFun").call(AbstractOverrideInSubinterfaceImpl()))

    return "OK"
}
