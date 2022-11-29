import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("function foo can not be called")) { bar() }
}
