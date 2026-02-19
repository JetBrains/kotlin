import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("oldFun") { Baz::oldFun.name }
}
