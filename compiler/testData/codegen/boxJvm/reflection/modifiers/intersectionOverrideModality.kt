// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.test.assertEquals

open class OpenClass {
    open fun openClassAbstractInterface() {}
}

interface AbstractInterface {
    fun openClassAbstractInterface()
}

class OpenClassAbstractInterface : OpenClass(), AbstractInterface


abstract class AbstractClass {
    abstract fun abstractClassDefaultInterface()
}

interface DefaultInterface {
    fun abstractClassDefaultInterface() {}
}

abstract class AbstractClassDefaultInterface : AbstractClass(), DefaultInterface


interface DefaultParentInterface {
    fun redeclaredAsAbstract() {}
}

interface AbstractChildInterface : DefaultParentInterface {
    override fun redeclaredAsAbstract()
}

interface AnotherAbstractInterface {
    fun redeclaredAsAbstract()
}

interface AbstractIntersection : AbstractChildInterface, AnotherAbstractInterface


open class FinalClass {
    fun finalClassAbstractInterface() {}
}

interface FinalClassAbstractInterface {
    fun finalClassAbstractInterface()
}

class FinalAndAbstract : FinalClass(), FinalClassAbstractInterface


private fun checkModality(kClass: KClass<*>, name: String, isAbstract: Boolean, isOpen: Boolean, isFinal: Boolean) {
    val function = kClass.members.single { it.name == name } as KFunction<*>
    assertEquals(isAbstract, function.isAbstract, "$function: isAbstract")
    assertEquals(isOpen, function.isOpen, "$function: isOpen")
    assertEquals(isFinal, function.isFinal, "$function: isFinal")
}

fun box(): String {
    checkModality(OpenClassAbstractInterface::class, "openClassAbstractInterface", false, true, false)
    checkModality(AbstractClassDefaultInterface::class, "abstractClassDefaultInterface", false, true, false)
    checkModality(AbstractIntersection::class, "redeclaredAsAbstract", true, false, false)
    checkModality(FinalAndAbstract::class, "finalClassAbstractInterface", false, false, true)
    return "OK"
}
