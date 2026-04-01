import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess { computeMemberExt() }
}
