import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(1) { getFunctionalInterfaceToInterface(1).answer() }
    expectSuccess(2) { getFunctionalInterfaceToInterfaceAsObject(2).answer() }
    expectSuccess(3) { getFunctionalInterfaceToInterfaceAnswer(3) }

    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith0AbstractFunctions' does not have an abstract function")) { getFunctionalInterfaceWith0AbstractFunctions(4).answer() }
    expectSuccess(5) { getFunctionalInterfaceWith0AbstractFunctionsAsObject(5).answer() }
    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith0AbstractFunctions' does not have an abstract function")) { getFunctionalInterfaceWith0AbstractFunctionsAnswer(6) }

    expectSuccess(7) { getFunctionalInterfaceWith1AbstractFunction(7).answer() }
    expectSuccess(8) { getFunctionalInterfaceWith1AbstractFunctionAsObject(8).answer() }
    expectSuccess(9) { getFunctionalInterfaceWith1AbstractFunctionAnswer(9) }

    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith2AbstractFunctions' has more than one abstract function: 'answer', 'function1'")) { getFunctionalInterfaceWith2AbstractFunctions(10).answer() }
    expectSuccess(11) { getFunctionalInterfaceWith2AbstractFunctionsAsObject(11).answer() }
    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith2AbstractFunctions' has more than one abstract function: 'answer', 'function1'")) { getFunctionalInterfaceWith2AbstractFunctionsAnswer(12) }

    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith3AbstractFunctions' has more than one abstract function: 'answer', 'function1', 'function2'")) { getFunctionalInterfaceWith3AbstractFunctions(13).answer() }
    expectSuccess(14) { getFunctionalInterfaceWith3AbstractFunctionsAsObject(14).answer() }
    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWith3AbstractFunctions' has more than one abstract function: 'answer', 'function1', 'function2'")) { getFunctionalInterfaceWith3AbstractFunctionsAnswer(15) }

    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWithAbstractProperty' has abstract property 'property1'")) { getFunctionalInterfaceWithAbstractProperty(16).answer() }
    expectSuccess(17) { getFunctionalInterfaceWithAbstractPropertyAsObject(17).answer() }
    /* TODO: KT-79371 */ if (!testMode.isNative) expectFailure(linkage("Single abstract method (SAM) conversion expression can not be evaluated: Fun interface 'FunctionalInterfaceWithAbstractProperty' has abstract property 'property1'")) { getFunctionalInterfaceWithAbstractPropertyAnswer(18) }
}
