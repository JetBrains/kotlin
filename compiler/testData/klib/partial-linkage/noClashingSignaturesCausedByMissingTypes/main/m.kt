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
}
