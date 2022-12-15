import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(linkage("Function exp can not be called: Function exp uses unlinked class symbol /E")) { d.bar() }
    expectSuccess { d.foo() }
}
