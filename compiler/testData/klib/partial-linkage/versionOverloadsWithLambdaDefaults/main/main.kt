import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { computeTrailing() }
    expectSuccess { computeArgument() }
}