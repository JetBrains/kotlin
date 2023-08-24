import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/removedFunction'")) { callRemovedOrNormalFunction(removed = true) }
    expectSuccess { callRemovedOrNormalFunction(removed = false) }
    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/A.removedFunction'")) { callRemovedOrNormalFunctionOnObject(removed = true) }
    expectSuccess { callRemovedOrNormalFunctionOnObject(removed = false) }

    expectFailure(linkage("Property accessor 'removedProperty.<get-removedProperty>' can not be called: No property accessor found for symbol '/removedProperty.<get-removedProperty>'")) { readRemovedOrNormalProperty(removed = true) }
    expectSuccess { readRemovedOrNormalProperty(removed = false) }
    expectFailure(linkage("Property accessor 'removedProperty1.<get-removedProperty1>' can not be called: No property accessor found for symbol '/A.removedProperty1.<get-removedProperty1>'")) { readRemovedOrNormalPropertyOnObject1(removed = true) }
    expectSuccess { readRemovedOrNormalPropertyOnObject1(removed = false) }
    expectFailure(linkage("Property accessor 'removedProperty2.<get-removedProperty2>' can not be called: No property accessor found for symbol '/A.removedProperty2.<get-removedProperty2>'")) { readRemovedOrNormalPropertyOnObject2(removed = true) }
    expectSuccess { readRemovedOrNormalPropertyOnObject2(removed = false) }

    expectFailure(linkage("Function 'removedFunction' can not be called: No function found for symbol '/removedFunction'")) { callInlinedRemovedFunction() }
    expectFailure(linkage("Property accessor 'removedProperty.<get-removedProperty>' can not be called: No property accessor found for symbol '/removedProperty.<get-removedProperty>'")) { readInlinedRemovedProperty() }

    expectSuccess { C2().removedOpenFunction() + I2().removedOpenFunction() }
    expectSuccess { C2().removedOpenProperty + I2().removedOpenProperty }
}
