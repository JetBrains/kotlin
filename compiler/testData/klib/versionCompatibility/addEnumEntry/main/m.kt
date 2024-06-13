import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { compute(E.UNCHANGED2) }
    expectFailure(noWhenBranch()) { compute(E.ADDED) }
    expectSuccess { compute(E.UNCHANGED1) }
}
