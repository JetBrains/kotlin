fun valChangeGet() = A.valChange
fun removedValGet() = A.removedVal
fun varChangeGet() = A.varChange
fun removedVarGet() = A.removedVar
fun removedVarSet() {
    A.removedVar = 0
}

val valChangeRef = A::valChange
val removedValRef = A::removedVal
val varChangeRef = A::varChange
val removedVarRef = A::removedVar

fun bodyChangeCall() = A.bodyChange()
fun removedFunCall() = A.removedFun()

val bodyChangeRef = A::bodyChange
val removedFunRef = A::removedFun

fun removedClassCall() = A.removedClass()
fun removedClassValueCall() = A.removedClassValue()
fun removedClassParameterCall() = A.removedClassParameter(RemovedClass(42))
fun removedClassTypeParameterCall() = A.removedClassTypeParameter<RemovedClass>()

fun removedCompanionValCall() = B.removedCompanionVal
fun removedCompanionVarCall() = B.removedCompanionVar
fun removedCompanionVarSet() {
    B.removedCompanionVar = 0
}
fun removedCompanionFunCall() = B.removedCompanionFun()

fun blockToObjectCall() = A.blockToObject()
fun objectToBlockCall() = A.objectToBlock()
fun blockToCompanionExtensionCall() = A.blockToCompanionExtension()
fun companionExtensionToBlockCall() = A.companionExtensionToBlock()
fun companionToRegularExtensionCall() = A.companionToRegularExtension()
fun regularToCompanionExtensionCall() = A().regularToCompanionExtension()
fun blockToRegularExtensionCall() = A.blockToRegularExtension()
fun regularExtensionToBlockCall() = A().regularExtensionToBlock()

fun noBlockSameFunCall() = RemovedBlock.sameFun()
fun newBlockSameFunCall() = NewBlock.sameFun()

fun privateClassCall() = PrivateClass.privateClassFun()
fun aliasCall() = A.aliasFun()
fun aliasToClassFunCall() = A.aliasToClassFun()
fun classToAliasFunCall() = B.classToAliasFun()
