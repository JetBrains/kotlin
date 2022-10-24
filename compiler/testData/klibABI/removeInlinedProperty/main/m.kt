import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("property accessor foo.<get-foo> can not be called")) { bar() }
}
