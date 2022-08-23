import abitestutils.abiTest

fun box() = abiTest {
    val c = ContainerImpl()
    expectSuccess(42) { publicToInternalFunction() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { publicToPrivateFunction() }
    expectSuccess(42) { c.publicToProtectedFunction() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { c.publicToInternalFunction() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { c.publicToPrivateFunction() }
    expectSuccess(42) { c.publicToProtectedFunctionAccess() }
    expectFailure(prefixed("function publicToInternalFunction can not be called")) { c.publicToInternalFunctionAccess() }
    expectFailure(prefixed("function publicToPrivateFunction can not be called")) { c.publicToPrivateFunctionAccess() }
    expectSuccess(42) { c.protectedToPublicFunctionAccess() }
    expectFailure(prefixed("function protectedToInternalFunction can not be called")) { c.protectedToInternalFunctionAccess() }
    expectFailure(prefixed("function protectedToPrivateFunction can not be called")) { c.protectedToPrivateFunctionAccess() }
}
