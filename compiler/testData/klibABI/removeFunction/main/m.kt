import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("function exp_foo can not be called")) { qux(true) }
    expectSuccess { qux(false) }
    expectFailure(prefixed("function exp_foo can not be called")) { qux2(true) }
    expectSuccess { qux2(false) }
}
