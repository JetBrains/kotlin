import abitestutils.abiTest

fun box() = abiTest {
    val c = Container()
    val ci = ContainerImpl()
    expectSuccess("publicToInternalTopLevelFunction.v2") { publicToInternalTopLevelFunction() }
    expectFailure(prefixed("function publicToPrivateTopLevelFunction can not be called")) { publicToPrivateTopLevelFunction() }
    expectSuccess("Container.publicToProtectedFunction.v2") { c.publicToProtectedFunction() }
    expectSuccess("Container.publicToProtectedFunction.v2") { ci.publicToProtectedFunction() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { c.publicToInternalFunction() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { ci.publicToInternalFunction() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { c.publicToPrivateFunction() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { ci.publicToPrivateFunction() }
    expectSuccess("Container.publicToProtectedFunction.v2") { ci.publicToProtectedFunctionAccess() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { ci.publicToInternalFunctionAccess() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { ci.publicToPrivateFunctionAccess() }
    expectSuccess("Container.protectedToPublicFunction.v2") { ci.protectedToPublicFunctionAccess() }
    expectFailure(prefixed("function protectedToInternalFunction can not be called")) { ci.protectedToInternalFunctionAccess() }
    expectFailure(prefixed("function protectedToPrivateFunction can not be called")) { ci.protectedToPrivateFunctionAccess() }
    expectSuccess("ContainerImpl.publicToProtectedOverriddenFunction") { ci.publicToProtectedOverriddenFunction() }
    expectSuccess("ContainerImpl.publicToInternalOverriddenFunction") { ci.publicToInternalOverriddenFunction() }
    expectSuccess("ContainerImpl.publicToPrivateOverriddenFunction") { ci.publicToPrivateOverriddenFunction() }
    expectSuccess("ContainerImpl.protectedToPublicOverriddenFunction") { ci.protectedToPublicOverriddenFunctionAccess() }
    expectSuccess("ContainerImpl.protectedToInternalOverriddenFunction") { ci.protectedToInternalOverriddenFunctionAccess() }
    expectSuccess("ContainerImpl.protectedToPrivateOverriddenFunction") { ci.protectedToPrivateOverriddenFunctionAccess() }
    expectSuccess("ContainerImpl.newPublicFunction") { ci.newPublicFunction() }
    expectSuccess("ContainerImpl.newOpenPublicFunction") { ci.newOpenPublicFunction() }
    expectSuccess("ContainerImpl.newProtectedFunction") { ci.newProtectedFunctionAccess() }
    expectSuccess("ContainerImpl.newOpenProtectedFunction") { ci.newOpenProtectedFunctionAccess() }
    expectSuccess("ContainerImpl.newInternalFunction") { ci.newInternalFunctionAccess() }
    expectSuccess("ContainerImpl.newOpenInternalFunction") { ci.newOpenInternalFunctionAccess() }
    expectSuccess("ContainerImpl.newPrivateFunction") { ci.newPrivateFunctionAccess() }
}
