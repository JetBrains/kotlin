import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Function exp_foo can not be called: No function found for symbol /exp_foo")) { qux(true) }
    expectSuccess { qux(false) }
    expectFailure(skipHashes("Function exp_foo can not be called: No function found for symbol /A.exp_foo")) { qux2(true) }
    expectSuccess { qux2(false) }
}
