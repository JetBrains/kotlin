import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("Any?") { Container().funExistingAnyOverload("") }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callfunExistingAnyOverloadWithRemovedClass() }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callFunTwoOverloadsWithRemovedClass() }
    expectFailure(linkage("Can not get instance of singleton 'RemovedEnum.A': No enum entry found for symbol '/RemovedEnum.A'")) { Container().callFunTwoOverloadsWithRemovedEnum() }

    expectSuccess("T : Any?") { Container().funExistingAnyOverloadTP<String>("") }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callfunExistingAnyOverloadWithRemovedClassTP() }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callFunTwoOverloadsWithRemovedClassTP() }
    expectFailure(linkage("Can not get instance of singleton 'RemovedEnum.A': No enum entry found for symbol '/RemovedEnum.A'")) { Container().callFunTwoOverloadsWithRemovedEnumTP() }

    expectSuccess("Any?") { Container().inlineFunExistingAnyOverload("") }
    expectSuccess("RemovedClass") { Container().callInlineFunExistingAnyOverloadWithRemovedClass() }

    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callFakeOverrideFunWithParameter() }
    expectFailure(linkage("Function 'funWithTypeParameter' can not be called: No function found for symbol '/Derived.funWithTypeParameter'")) { Container().callFakeOverrideFunWithTypeParameter() }
    expectFailure(linkage("Constructor 'RemovedClass.<init>' can not be called: No constructor found for symbol '/RemovedClass.<init>'")) { Container().callFakeOverrideWithAnyParameter() }
    expectFailure(linkage("Function 'funWithAnyTypeParameter' can not be called: Expression uses unlinked class symbol '/RemovedClass'")) { Container().callFakeOverrideWithAnyTypeParameter() }

}
