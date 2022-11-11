import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(skipHashes("Function exp can not be called: Function exp uses unlinked class symbol /E (through class EX)")) { d.bar() }
    expectSuccess { d.foo() }
}
