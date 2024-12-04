interface InterfaceToAbstractClass
interface InterfaceToOpenClass
interface InterfaceToFinalClass
interface InterfaceToAnnotationClass
interface InterfaceToObject
interface InterfaceToEnumClass
interface InterfaceToValueClass
interface InterfaceToDataClass

open class OpenClassToFinalClass(val x: Int)
open class OpenClassToAnnotationClass(val x: Int)
open class OpenClassToObject(val x: Int)
open class OpenClassToEnumClass(val x: Int)
open class OpenClassToValueClass(val x: Int)
open class OpenClassToDataClass(val x: Int)
open class OpenClassToInterface(val x: Int)

interface InterfaceToAbstractClass1
interface InterfaceToAbstractClass2
abstract class AbstractClass

interface RemovedInterface {
    fun abstractFun(): String
    fun abstractFunWithDefaultImpl(): String = "RemovedInterface.abstractFunWithDefaultImpl"
    val abstractVal: String
    val abstractValWithDefaultImpl: String get() = "RemovedInterface.abstractValWithDefaultImpl"
}

abstract class RemovedAbstractClass {
    abstract fun abstractFun(): String
    open fun openFun(): String = "RemovedAbstractClass.openFun"
    fun finalFun(): String = "RemovedAbstractClass.finalFun"
    abstract val abstractVal: String
    open val openVal: String get() = "RemovedAbstractClass.openVal"
    val finalVal: String get() = "RemovedAbstractClass.finalVal"
}

open class RemovedOpenClass {
    open fun openFun(): String = "RemovedOpenClass.openFun"
    fun finalFun(): String = "RemovedOpenClass.finalFun"
    open val openVal: String get() = "RemovedOpenClass.openVal"
    val finalVal: String get() = "RemovedOpenClass.finalVal"
}

abstract class AbstractClassWithChangedConstructorSignature(name: String) {
    val greeting = "Hello, $name!"
}

open class OpenClassWithChangedConstructorSignature(name: String) {
    val greeting = "Hello, $name!"
}

open class SuperSuperClass {
    open fun inheritsFrom() = "SuperSuperClass -> Any"
}
open class SuperClass : SuperSuperClass() {
    override fun inheritsFrom() = "SuperClass -> " + super.inheritsFrom()
}
class SuperSuperClassReplacedBySuperClass : SuperSuperClass() {
    override fun inheritsFrom() = "SuperSuperClassReplacedBySuperClass -> " + super.inheritsFrom()
}
class SuperClassReplacedBySuperSuperClass : SuperClass() {
    override fun inheritsFrom() = "SuperClassReplacedBySuperSuperClass -> " + super.inheritsFrom()
}
