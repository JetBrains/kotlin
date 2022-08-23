import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("property accessor exp_foo.<get-exp_foo> can not be called")) { qux(true) }
    expectSuccess { qux(false) }
    expectFailure(prefixed("property accessor exp_foo.<get-exp_foo> can not be called")) { qux2(true) }
    expectSuccess { qux2(false) }
    expectFailure(prefixed("property accessor exp_foo.<get-exp_foo> can not be called")) { qux3(true) }
    expectSuccess { qux3(false) }
}
