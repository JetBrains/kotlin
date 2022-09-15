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
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty1") { c.publicToProtectedOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty2") { c.publicToProtectedOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty3") { c.publicToProtectedOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty4") { c.publicToProtectedOverriddenProperty4 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty1") { c.publicToInternalOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty2") { c.publicToInternalOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty3") { c.publicToInternalOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty4") { c.publicToInternalOverriddenProperty4 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty1") { c.publicToPrivateOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty2") { c.publicToPrivateOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty3") { c.publicToPrivateOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty4") { c.publicToPrivateOverriddenProperty4 }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty1") { c.protectedToPublicOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty2") { c.protectedToPublicOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty3") { c.protectedToPublicOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty4") { c.protectedToPublicOverriddenProperty4Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty1") { c.protectedToInternalOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty2") { c.protectedToInternalOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty3") { c.protectedToInternalOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty4") { c.protectedToInternalOverriddenProperty4Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty1") { c.protectedToPrivateOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty2") { c.protectedToPrivateOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty3") { c.protectedToPrivateOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty4") { c.protectedToPrivateOverriddenProperty4Access() }
}
