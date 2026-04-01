import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { computeC() }
    expectSuccess { computeD() }
}