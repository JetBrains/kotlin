import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(linkage("Function 'exp' can not be called: Function uses unlinked class symbol '/E'")) { d.bar() }
    expectSuccess { d.foo() }
    expectFailure(linkage("Property accessor 'exp.<set-exp>' can not be called: Property accessor uses unlinked class symbol '/E'")) { d.baz() }
}
