import abitestutils.abiTest

fun box() = abiTest {
    val c = ContainerImpl()
    expectSuccess("publicToInternalTopLevelProperty1.v2") { publicToInternalTopLevelProperty1 }
    expectSuccess("publicToInternalTopLevelProperty2.v2") { publicToInternalTopLevelProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty1.<get-publicToPrivateTopLevelProperty1> can not be called")) { publicToPrivateTopLevelProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty2.<get-publicToPrivateTopLevelProperty2> can not be called")) { publicToPrivateTopLevelProperty2 }
    expectSuccess("Container.publicToProtectedProperty1.v2") { c.publicToProtectedProperty1 }
    expectSuccess("Container.publicToProtectedProperty2.v2") { c.publicToProtectedProperty2 }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { c.publicToInternalProperty1 }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { c.publicToInternalProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { c.publicToPrivateProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { c.publicToPrivateProperty2 }
    expectSuccess("Container.publicToProtectedProperty1.v2") { c.publicToProtectedProperty1Access() }
    expectSuccess("Container.publicToProtectedProperty2.v2") { c.publicToProtectedProperty2Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { c.publicToInternalProperty1Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { c.publicToInternalProperty2Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { c.publicToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { c.publicToPrivateProperty2Access() }
    expectSuccess("Container.protectedToPublicProperty1.v2") { c.protectedToPublicProperty1Access() }
    expectSuccess("Container.protectedToPublicProperty2.v2") { c.protectedToPublicProperty2Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty1.<get-protectedToInternalProperty1> can not be called")) { c.protectedToInternalProperty1Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty2.<get-protectedToInternalProperty2> can not be called")) { c.protectedToInternalProperty2Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty1.<get-protectedToPrivateProperty1> can not be called")) { c.protectedToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty2.<get-protectedToPrivateProperty2> can not be called")) { c.protectedToPrivateProperty2Access() }
}
