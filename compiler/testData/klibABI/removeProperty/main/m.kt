import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Property accessor exp_foo.<get-exp_foo> can not be called: No property accessor found for symbol /exp_foo.<get-exp_foo>")) { qux(true) }
    expectSuccess { qux(false) }
    expectFailure(linkage("Property accessor exp_foo.<get-exp_foo> can not be called: No property accessor found for symbol /A.exp_foo.<get-exp_foo>")) { qux2(true) }
    expectSuccess { qux2(false) }
    expectFailure(linkage("Property accessor exp_foo.<get-exp_foo> can not be called: No property accessor found for symbol /B.exp_foo.<get-exp_foo>")) { qux3(true) }
    expectSuccess { qux3(false) }
}
