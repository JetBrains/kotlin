import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("foo") { removedFunName() }
    expectFailure(
        linkage("Reference to function 'foo' can not be evaluated: No function found for symbol '/Baz.foo'")
    ) { removedFunString() }
    expectFailure(
        linkage("Function 'foo' can not be called: No function found for symbol '/Baz.foo'")
    ) { removedFunCall() }
}
