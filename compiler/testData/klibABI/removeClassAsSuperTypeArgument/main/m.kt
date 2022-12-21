import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(linkage("Function 'exp' can not be called: Function uses unlinked class symbol '/E' (through class 'EX')")) { d.bar() }
    expectSuccess { d.foo() }
}
