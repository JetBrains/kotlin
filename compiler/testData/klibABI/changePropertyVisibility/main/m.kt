import abitestutils.abiTest

fun box() = abiTest {
    val c = ContainerImpl()
    expectSuccess(42) { publicToInternalTopLevelProperty1 }
    expectSuccess(42) { publicToInternalTopLevelProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty1.<get-publicToPrivateTopLevelProperty1> can not be called")) { publicToPrivateTopLevelProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty2.<get-publicToPrivateTopLevelProperty2> can not be called")) { publicToPrivateTopLevelProperty2 }
    expectSuccess(42) { c.publicToProtectedProperty1 }
    expectSuccess(42) { c.publicToProtectedProperty2 }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { c.publicToInternalProperty1 }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { c.publicToInternalProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { c.publicToPrivateProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { c.publicToPrivateProperty2 }
    expectSuccess(42) { c.publicToProtectedProperty1Access() }
    expectSuccess(42) { c.publicToProtectedProperty2Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { c.publicToInternalProperty1Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { c.publicToInternalProperty2Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { c.publicToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { c.publicToPrivateProperty2Access() }
    expectSuccess(42) { c.protectedToPublicProperty1Access() }
    expectSuccess(42) { c.protectedToPublicProperty2Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty1.<get-protectedToInternalProperty1> can not be called")) { c.protectedToInternalProperty1Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty2.<get-protectedToInternalProperty2> can not be called")) { c.protectedToInternalProperty2Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty1.<get-protectedToPrivateProperty1> can not be called")) { c.protectedToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty2.<get-protectedToPrivateProperty2> can not be called")) { c.protectedToPrivateProperty2Access() }
}
