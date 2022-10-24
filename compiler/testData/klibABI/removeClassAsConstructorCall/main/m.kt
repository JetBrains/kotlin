import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("constructor Foo.<init> can not be called")) { bar() }
}
