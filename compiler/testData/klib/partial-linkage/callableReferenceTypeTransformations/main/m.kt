import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceToInterface' can not be evaluated: Interface 'FunctionalInterfaceToInterface' is not a fun interface")) { getFunctionalInterfaceToInterface(1).answer() }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Interface 'FunctionalInterfaceToInterface' is not a fun interface")) { getFunctionalInterfaceToInterfaceAsSamConverted(2).answer() }
    expectSuccess(3) { getFunctionalInterfaceToInterfaceAsObject(3).answer() }
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceToInterface' can not be evaluated: Interface 'FunctionalInterfaceToInterface' is not a fun interface")) { getFunctionalInterfaceToInterfaceAnswer(4) }

    // The difference between backends comes from how they represent (immidiate) SAM conversions in IR (see UpgradeCallableReferences.upgradeSamConversions).
    // Ideally there should be no difference, but it is acceptable for now.
    if (testMode.isNative) {
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithChangedFun' can not be evaluated: No function found for symbol '/FunInterfaceWithChangedFun.answer'")) { getFunInterfaceWithChangedFun(1); "OK" }
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithChangedFun' can not be evaluated: No function found for symbol '/FunInterfaceWithChangedFun.answer'")) { getFunInterfaceWithChangedFunAnswer(2) }
    } else {
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithChangedFun' can not be evaluated: Cannot convert from function 'invoke' to function 'answer'")) { getFunInterfaceWithChangedFun(1); "OK" }
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithChangedFun' can not be evaluated: Cannot convert from function 'invoke' to function 'answer'")) { getFunInterfaceWithChangedFunAnswer(2) }
    }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Cannot convert from function 'invoke' to function 'answer'")) { getFunInterfaceWithChangedFunAsSamConverted(3); "OK" }
    expectFailure(linkage("Function 'answer' can not be called: No function found for symbol '/FunInterfaceWithChangedFun.answer'")) { getFunInterfaceWithChangedFunAsObject(4).answer() }

    // The difference between backends comes from how they represent (immidiate) SAM conversions in IR (see UpgradeCallableReferences.upgradeSamConversions).
    // Ideally there should be no difference, but it is acceptable for now.
    if (testMode.isNative) {
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentAbstractFun' can not be evaluated: The single abstract method of fun interface 'FunInterfaceWithDifferentAbstractFun' changed from function 'answer' to function 'hijack'")) { getFunInterfaceWithDifferentAbstractFun(1).answer() }
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentAbstractFun' can not be evaluated: The single abstract method of fun interface 'FunInterfaceWithDifferentAbstractFun' changed from function 'answer' to function 'hijack'")) { getFunInterfaceWithDifferentAbstractFunAnswer(4) }
    } else {
        expectSuccess(42) { getFunInterfaceWithDifferentAbstractFun(1).answer() }
        expectSuccess(42) { getFunInterfaceWithDifferentAbstractFunAnswer(2) }
    }
    expectSuccess(42) { getFunInterfaceWithDifferentAbstractFunAsSamConverted(3).answer() }
    expectSuccess(4) { getFunInterfaceWithDifferentAbstractFunAsObject(4).answer() }

    // The difference between backends comes from how they represent (immidiate) SAM conversions in IR (see UpgradeCallableReferences.upgradeSamConversions).
    // Ideally there should be no difference, but it is acceptable for now.
    if (testMode.isNative) {
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentChangedAbstractFun' can not be evaluated: The single abstract method of fun interface 'FunInterfaceWithDifferentChangedAbstractFun' changed from function 'answer' to function 'hijack'")) { getFunInterfaceWithDifferentChangedAbstractFun(1).answer() }
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentChangedAbstractFun' can not be evaluated: The single abstract method of fun interface 'FunInterfaceWithDifferentChangedAbstractFun' changed from function 'answer' to function 'hijack'")) { getFunInterfaceWithDifferentChangedAbstractFunAnswer(4) }
    } else {
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentChangedAbstractFun' can not be evaluated: Cannot convert from function 'invoke' to function 'hijack'")) { getFunInterfaceWithDifferentChangedAbstractFun(1).answer() }
        expectFailure(linkage("Reference to lambda in function 'getFunInterfaceWithDifferentChangedAbstractFun' can not be evaluated: Cannot convert from function 'invoke' to function 'hijack'")) { getFunInterfaceWithDifferentChangedAbstractFunAnswer(4) }
    }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Cannot convert from function 'invoke' to function 'hijack'")) { getFunInterfaceWithDifferentChangedAbstractFunAsSamConverted(3).answer() }
    expectSuccess(4) { getFunInterfaceWithDifferentChangedAbstractFunAsObject(4).answer() }

    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith0AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith0AbstractFunctions' does not have an abstract function")) { getFunctionalInterfaceWith0AbstractFunctions(4).answer() }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith0AbstractFunctions' does not have an abstract function")) { getFunctionalInterfaceWith0AbstractFunctionsAsSamConverted(11).answer() }
    expectSuccess(5) { getFunctionalInterfaceWith0AbstractFunctionsAsObject(5).answer() }
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith0AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith0AbstractFunctions' does not have an abstract function")) { getFunctionalInterfaceWith0AbstractFunctionsAnswer(6) }

    expectSuccess(7) { getFunctionalInterfaceWith1AbstractFunction(7).answer() }
    expectSuccess(8) { getFunctionalInterfaceWith1AbstractFunctionAsSamConverted(8).answer() }
    expectSuccess(9) { getFunctionalInterfaceWith1AbstractFunctionAsObject(9).answer() }
    expectSuccess(10) { getFunctionalInterfaceWith1AbstractFunctionAnswer(10) }

    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith2AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith2AbstractFunctions' has more than one abstract function: 'answer', 'function1'")) { getFunctionalInterfaceWith2AbstractFunctions(10).answer() }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith2AbstractFunctions' has more than one abstract function: 'answer', 'function1'")) { getFunctionalInterfaceWith2AbstractFunctionsAsSamConverted(11).answer() }
    expectSuccess(11) { getFunctionalInterfaceWith2AbstractFunctionsAsObject(11).answer() }
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith2AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith2AbstractFunctions' has more than one abstract function: 'answer', 'function1'")) { getFunctionalInterfaceWith2AbstractFunctionsAnswer(12) }

    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith3AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith3AbstractFunctions' has more than one abstract function: 'answer', 'function1', 'function2'")) { getFunctionalInterfaceWith3AbstractFunctions(13).answer() }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith3AbstractFunctions' has more than one abstract function: 'answer', 'function1', 'function2'")) { getFunctionalInterfaceWith3AbstractFunctionsAsSamConverted(11).answer() }
    expectSuccess(14) { getFunctionalInterfaceWith3AbstractFunctionsAsObject(14).answer() }
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWith3AbstractFunctions' can not be evaluated: Fun interface 'FunctionalInterfaceWith3AbstractFunctions' has more than one abstract function: 'answer', 'function1', 'function2'")) { getFunctionalInterfaceWith3AbstractFunctionsAnswer(15) }

    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWithAbstractProperty' can not be evaluated: Fun interface 'FunctionalInterfaceWithAbstractProperty' has abstract property 'property1'")) { getFunctionalInterfaceWithAbstractProperty(16).answer() }
    expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWithAbstractProperty' has abstract property 'property1'")) { getFunctionalInterfaceWithAbstractPropertyAsSamConverted(11).answer() }
    expectSuccess(17) { getFunctionalInterfaceWithAbstractPropertyAsObject(17).answer() }
    expectFailure(linkage("Reference to lambda in function 'getFunctionalInterfaceWithAbstractProperty' can not be evaluated: Fun interface 'FunctionalInterfaceWithAbstractProperty' has abstract property 'property1'")) { getFunctionalInterfaceWithAbstractPropertyAnswer(18) }
}
