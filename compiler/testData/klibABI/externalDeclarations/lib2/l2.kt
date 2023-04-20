@Suppress("ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED")
external class ExternalClassInheritedFromAbstractExternalClass : AbstractExternalClass {
//    override fun abstractFunction(): String
    override fun removedAbstractFunction(): String
}

fun ExternalClassInheritedFromAbstractExternalClass.callRemovedFunction() = removedFunction()

class RegularClassInheritedFromAbstractExternalClass : AbstractExternalClass() {
    override fun abstractFunction() = "RegularClassInheritedFromAbstractExternalClass.abstractFunction"
    override fun removedAbstractFunction() = "RegularClassInheritedFromAbstractExternalClass.removedAbstractFunction"
}

fun RegularClassInheritedFromAbstractExternalClass.callRemovedFunction() = removedFunction()

open external class OpenExternalClass {
    fun function(): String
}

@Suppress("INTERFACE_WITH_SUPERCLASS")
external interface ExternalInterfaceInheritedFromOpenExternalClass : OpenExternalClass {
    fun abstractFunction(): String
}

external class ExternalClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass : ExternalInterfaceInheritedFromOpenExternalClass {
    override fun abstractFunction(): String
}

class RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass :
    ExternalInterfaceInheritedFromOpenExternalClass,
    /* Note: Have to explicitly inherit from 'OpenExternalClass' becase otherwise JS codegen sees that
     * class 'RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass' is inherited only from
     * interface 'ExternalInterfaceInheritedFromOpenExternalClass' and does not figure out that a bridge has to be
     * generated for function 'function' inherited from 'OpenExternalClass'.
     */
    OpenExternalClass() {
    override fun abstractFunction(): String = "RegularClassInheritedFromExternalInterfaceInheritedFromOpenExternalClass.abstractFunction"
}
