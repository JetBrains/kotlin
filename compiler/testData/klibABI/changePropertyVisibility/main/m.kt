import abitestutils.abiTest

fun box() = abiTest {
    val c = Container()
    val ci = ContainerImpl()
    expectSuccess("publicToInternalTopLevelProperty1.v2") { publicToInternalTopLevelProperty1 }
    expectSuccess("publicToInternalTopLevelProperty2.v2") { publicToInternalTopLevelProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty1.<get-publicToPrivateTopLevelProperty1> can not be called")) { publicToPrivateTopLevelProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateTopLevelProperty2.<get-publicToPrivateTopLevelProperty2> can not be called")) { publicToPrivateTopLevelProperty2 }
    expectSuccess("Container.publicToProtectedProperty1.v2") { c.publicToProtectedProperty1 }
    expectSuccess("Container.publicToProtectedProperty1.v2") { ci.publicToProtectedProperty1 }
    expectSuccess("Container.publicToProtectedProperty2.v2") { c.publicToProtectedProperty2 }
    expectSuccess("Container.publicToProtectedProperty2.v2") { ci.publicToProtectedProperty2 }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { c.publicToInternalProperty1 }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { ci.publicToInternalProperty1 }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { c.publicToInternalProperty2 }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { ci.publicToInternalProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { c.publicToPrivateProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { ci.publicToPrivateProperty1 }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { c.publicToPrivateProperty2 }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { ci.publicToPrivateProperty2 }
    expectSuccess("Container.publicToProtectedProperty1.v2") { ci.publicToProtectedProperty1Access() }
    expectSuccess("Container.publicToProtectedProperty2.v2") { ci.publicToProtectedProperty2Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")) { ci.publicToInternalProperty1Access() }
    expectFailure(prefixed("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")) { ci.publicToInternalProperty2Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")) { ci.publicToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")) { ci.publicToPrivateProperty2Access() }
    expectSuccess("Container.protectedToPublicProperty1.v2") { ci.protectedToPublicProperty1Access() }
    expectSuccess("Container.protectedToPublicProperty2.v2") { ci.protectedToPublicProperty2Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty1.<get-protectedToInternalProperty1> can not be called")) { ci.protectedToInternalProperty1Access() }
    expectFailure(prefixed("property accessor protectedToInternalProperty2.<get-protectedToInternalProperty2> can not be called")) { ci.protectedToInternalProperty2Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty1.<get-protectedToPrivateProperty1> can not be called")) { ci.protectedToPrivateProperty1Access() }
    expectFailure(prefixed("property accessor protectedToPrivateProperty2.<get-protectedToPrivateProperty2> can not be called")) { ci.protectedToPrivateProperty2Access() }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty1") { ci.publicToProtectedOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty2") { ci.publicToProtectedOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty3") { ci.publicToProtectedOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenProperty4") { ci.publicToProtectedOverriddenProperty4 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty1") { ci.publicToInternalOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty2") { ci.publicToInternalOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty3") { ci.publicToInternalOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToInternalOverriddenProperty4") { ci.publicToInternalOverriddenProperty4 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty1") { ci.publicToPrivateOverriddenProperty1 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty2") { ci.publicToPrivateOverriddenProperty2 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty3") { ci.publicToPrivateOverriddenProperty3 }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenProperty4") { ci.publicToPrivateOverriddenProperty4 }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty1") { ci.protectedToPublicOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty2") { ci.protectedToPublicOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty3") { ci.protectedToPublicOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenProperty4") { ci.protectedToPublicOverriddenProperty4Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty1") { ci.protectedToInternalOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty2") { ci.protectedToInternalOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty3") { ci.protectedToInternalOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenProperty4") { ci.protectedToInternalOverriddenProperty4Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty1") { ci.protectedToPrivateOverriddenProperty1Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty2") { ci.protectedToPrivateOverriddenProperty2Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty3") { ci.protectedToPrivateOverriddenProperty3Access() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenProperty4") { ci.protectedToPrivateOverriddenProperty4Access() }
    expectSuccess("ContainerImpl.newPublicProperty1") { ci.newPublicProperty1 }
    expectSuccess("ContainerImpl.newPublicProperty2") { ci.newPublicProperty2 }
    expectSuccess("ContainerImpl.newPublicProperty3") { ci.newPublicProperty3 }
    expectSuccess("ContainerImpl.newPublicProperty4") { ci.newPublicProperty4 }
    expectSuccess("ContainerImpl.newProtectedProperty1") { ci.newProtectedProperty1Access() }
    expectSuccess("ContainerImpl.newProtectedProperty2") { ci.newProtectedProperty2Access() }
    expectSuccess("ContainerImpl.newProtectedProperty3") { ci.newProtectedProperty3Access() }
    expectSuccess("ContainerImpl.newProtectedProperty4") { ci.newProtectedProperty4Access() }
    expectSuccess("ContainerImpl.newOpenProtectedProperty1") { ci.newOpenProtectedProperty1Access() }
    expectSuccess("ContainerImpl.newOpenProtectedProperty2") { ci.newOpenProtectedProperty2Access() }
    expectSuccess("ContainerImpl.newOpenProtectedProperty3") { ci.newOpenProtectedProperty3Access() }
    expectSuccess("ContainerImpl.newOpenProtectedProperty4") { ci.newOpenProtectedProperty4Access() }
    expectSuccess("ContainerImpl.newInternalProperty1") { ci.newInternalProperty1Access() }
    expectSuccess("ContainerImpl.newInternalProperty2") { ci.newInternalProperty2Access() }
    expectSuccess("ContainerImpl.newInternalProperty3") { ci.newInternalProperty3Access() }
    expectSuccess("ContainerImpl.newInternalProperty4") { ci.newInternalProperty4Access() }
    expectSuccess("ContainerImpl.newOpenInternalProperty1") { ci.newOpenInternalProperty1Access() }
    expectSuccess("ContainerImpl.newOpenInternalProperty2") { ci.newOpenInternalProperty2Access() }
    expectSuccess("ContainerImpl.newOpenInternalProperty3") { ci.newOpenInternalProperty3Access() }
    expectSuccess("ContainerImpl.newOpenInternalProperty4") { ci.newOpenInternalProperty4Access() }
    expectSuccess("ContainerImpl.newPrivateProperty1") { ci.newPrivateProperty1Access() }
    expectSuccess("ContainerImpl.newPrivateProperty2") { ci.newPrivateProperty2Access() }
    expectSuccess("ContainerImpl.newPrivateProperty3") { ci.newPrivateProperty3Access() }
    expectSuccess("ContainerImpl.newPrivateProperty4") { ci.newPrivateProperty4Access() }
}
