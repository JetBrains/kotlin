import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(prefixed("function exp can not be called")) { d.bar() }
    expectSuccess { d.foo() }
}
