import abitestutils.abiTest

fun box() = abiTest {
    val d = D()
    expectFailure(skipHashes("Function exp can not be called: Function exp uses unlinked class symbol /E")) { d.bar() }
    expectSuccess { d.foo() }
    expectFailure(skipHashes("Property accessor exp.<set-exp> can not be called: Property accessor exp.<set-exp> uses unlinked class symbol /E")) { d.baz() }
}
