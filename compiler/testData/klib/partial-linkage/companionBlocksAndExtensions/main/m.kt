import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("propertyChange.v2") { valChangeGet() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/A.removedVal.<get-removedVal>'")) { removedValGet() }
    expectSuccess("propertyChange.v2") { varChangeGet() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<get-removedVar>'")) { removedVarGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<set-removedVar>'")) { removedVarSet() }

    expectSuccess("propertyChange.v2") { valChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/A.removedVal.<get-removedVal>'")) { removedValRef.invoke() }
    expectSuccess("propertyChange.v2") { varChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<get-removedVar>'")) { removedVarRef.invoke() }

    expectSuccess("bodyChange.v2") { bodyChangeCall() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/A.removedFun'")) { removedFunCall() }

    expectSuccess("bodyChange.v2") { bodyChangeRef.invoke() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/A.removedFun'")) { removedFunRef.invoke() }

    expectSuccess("extensionPropertyChange.v2") { extensionValChangeGet() }
    expectFailure(linkage("Property accessor 'removedExtensionVal.<get-removedExtensionVal>' can not be called: No property accessor found for symbol '/removedExtensionVal.<get-removedExtensionVal>#companion@A'")) { removedExtensionValGet() }
    expectSuccess("extensionPropertyChange.v2") { extensionVarChangeGet() }
    expectFailure(linkage("Property accessor 'removedExtensionVar.<get-removedExtensionVar>' can not be called: No property accessor found for symbol '/removedExtensionVar.<get-removedExtensionVar>#companion@A'")) { removedExtensionVarGet() }
    expectFailure(linkage("Property accessor 'removedExtensionVar.<set-removedExtensionVar>' can not be called: No property accessor found for symbol '/removedExtensionVar.<set-removedExtensionVar>#companion@A'")) { removedExtensionVarSet() }

    expectSuccess("extensionPropertyChange.v2") { extensionValChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedExtensionVal.<get-removedExtensionVal>' can not be called: No property accessor found for symbol '/removedExtensionVal.<get-removedExtensionVal>#companion@A'")) { removedExtensionValRef.invoke() }
    expectSuccess("extensionPropertyChange.v2") { extensionVarChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedExtensionVar.<get-removedExtensionVar>' can not be called: No property accessor found for symbol '/removedExtensionVar.<get-removedExtensionVar>#companion@A'")) { removedExtensionVarRef.invoke() }

    expectSuccess("extensionFunBodyChange.v2") { extensionFunBodyChangeCall() }
    expectFailure(linkage("Function 'removedExtensionFun' can not be called: No function found for symbol '/removedExtensionFun#companion@A'")) { removedExtensionFunCall() }

    expectSuccess("extensionFunBodyChange.v2") { extensionFunBodyChangeRef.invoke() }
    expectFailure(linkage("Function 'removedExtensionFun' can not be called: No function found for symbol '/removedExtensionFun#companion@A'")) { removedExtensionFunRef.invoke() }

    expectSuccess("removedClass") { removedClassCall() }
    expectSuccess(42) { removedClassValueCall() }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { removedClassParameterCall() }
    expectFailure(linkage("Function 'removedClassTypeParameter' can not be called: No function found for symbol '/A.removedClassTypeParameter'")) { removedClassTypeParameterCall() }

    expectFailure(linkage("Property accessor 'removedCompanionVal.<get-removedCompanionVal>' can not be called: No property accessor found for symbol '/B.removedCompanionVal.<get-removedCompanionVal>'")) { removedCompanionValCall() }
    expectFailure(linkage("Property accessor 'removedCompanionVar.<get-removedCompanionVar>' can not be called: No property accessor found for symbol '/B.removedCompanionVar.<get-removedCompanionVar>'")) { removedCompanionVarCall() }
    expectFailure(linkage("Property accessor 'removedCompanionVar.<set-removedCompanionVar>' can not be called: No property accessor found for symbol '/B.removedCompanionVar.<set-removedCompanionVar>'")) { removedCompanionVarSet() }
    expectFailure(linkage("Function 'removedCompanionFun' can not be called: No function found for symbol '/B.removedCompanionFun'")) { removedCompanionFunCall() }
    expectFailure(linkage("Property accessor 'removedCompanionVal.<get-removedCompanionVal>' can not be called: No property accessor found for symbol '/B.removedCompanionVal.<get-removedCompanionVal>'")) { removedCompanionValRef.invoke() }
    expectFailure(linkage("Property accessor 'removedCompanionVar.<get-removedCompanionVar>' can not be called: No property accessor found for symbol '/B.removedCompanionVar.<get-removedCompanionVar>'")) { removedCompanionVarRef.invoke() }
    expectFailure(linkage("Function 'removedCompanionFun' can not be called: No function found for symbol '/B.removedCompanionFun'")) { removedCompanionFunRef.invoke() }


    expectFailure(linkage("Function 'blockToObject' can not be called: No function found for symbol '/A.blockToObject'")) { blockToObjectCall() }
    expectFailure(linkage("Function 'objectToBlock' can not be called: No function found for symbol '/A.Companion.objectToBlock'")) { objectToBlockCall() }
    expectFailure(linkage("Function 'blockToCompanionExtension' can not be called: No function found for symbol '/A.blockToCompanionExtension'")) { blockToCompanionExtensionCall() }
    expectFailure(linkage("Function 'companionExtensionToBlock' can not be called: No function found for symbol '/companionExtensionToBlock#companion@A'")) { companionExtensionToBlockCall() }
    expectFailure(linkage("Function 'companionToRegularExtension' can not be called: No function found for symbol '/companionToRegularExtension#companion@A'")) { companionToRegularExtensionCall() }
    expectFailure(linkage("Function 'regularToCompanionExtension' can not be called: No function found for symbol '/regularToCompanionExtension'")) { regularToCompanionExtensionCall() }
    expectFailure(linkage("Function 'blockToRegularExtension' can not be called: No function found for symbol '/A.blockToRegularExtension'")) { blockToRegularExtensionCall() }
    expectFailure(linkage("Function 'regularExtensionToBlock' can not be called: No function found for symbol '/regularExtensionToBlock'")) { regularExtensionToBlockCall() }

    expectFailure(linkage("Function 'sameFun' can not be called: No function found for symbol '/RemovedBlock.sameFun'")) { noBlockSameFunCall() }
    expectSuccess("object") { newBlockSameFunCall() }

    expectFailure(linkage("Function 'privateClassFun' can not be called: No function found for symbol '/privateClassFun#companion@PrivateClass'")) { privateClassCall() }
    expectFailure(linkage("Function 'aliasFun' can not be called: No function found for symbol '/aliasFun#companion@A'")) { aliasCall() }
    expectSuccess("aliasToClassFun") { aliasToClassFunCall() }
    expectSuccess("classToAliasFun") { classToAliasFunCall() }

    // To follow the JVM rules, moving companion members to a superclass is ABI compatible, but moving to a superinterface is not.
    expectSuccess("moved") { funMovedToParentClass() }
    expectSuccess(42) { propMovedToParentClass() }
    expectFailure(linkage("Function 'funMovedToParentInterface' can not be called: No function found for symbol '/Derived.funMovedToParentInterface'")) { funMovedToParentInterface() }
    expectFailure(linkage("Property accessor 'propMovedToParentInterface.<get-propMovedToParentInterface>' can not be called: No property accessor found for symbol '/Derived.propMovedToParentInterface.<get-propMovedToParentInterface>'")) { propMovedToParentInterface() }
}
