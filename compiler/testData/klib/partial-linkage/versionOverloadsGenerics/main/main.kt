import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { computeFoo() }
    expectSuccess { computeBar() }
}
