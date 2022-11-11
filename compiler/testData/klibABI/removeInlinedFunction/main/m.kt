import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Function foo can not be called: No function found for symbol /foo")) { bar() }
}
