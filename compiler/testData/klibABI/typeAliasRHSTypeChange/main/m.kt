import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Function foo can not be called: No function found for symbol /foo")) { callFoo() }
    expectSuccess { bar() }
}
