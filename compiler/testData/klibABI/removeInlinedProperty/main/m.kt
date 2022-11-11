import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Property accessor foo.<get-foo> can not be called: No property accessor found for symbol /foo.<get-foo>")) { bar() }
}
