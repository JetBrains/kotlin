import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(prefixed("constructor E.<init> can not be called")) { d.bar() }
    expectSuccess { d.foo() }
}
