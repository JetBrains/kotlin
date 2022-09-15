import abitestutils.abiTest

fun box() = abiTest {
    val c = ContainerImpl()
    expectSuccess("publicToInternalTopLevelFunction.v2") { publicToInternalTopLevelFunction() }
    expectFailure(prefixed("function publicToPrivateTopLevelFunction can not be called")) { publicToPrivateTopLevelFunction() }
    expectSuccess("Container.publicToProtectedFunction.v2") { c.publicToProtectedFunction() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { c.publicToInternalFunction() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { c.publicToPrivateFunction() }
    expectSuccess("Container.publicToProtectedFunction.v2") { c.publicToProtectedFunctionAccess() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { c.publicToInternalFunctionAccess() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { c.publicToPrivateFunctionAccess() }
    expectSuccess("Container.protectedToPublicFunction.v2") { c.protectedToPublicFunctionAccess() }
    expectFailure(prefixed("function protectedToInternalFunction can not be called")) { c.protectedToInternalFunctionAccess() }
    expectFailure(prefixed("function protectedToPrivateFunction can not be called")) { c.protectedToPrivateFunctionAccess() }
}
