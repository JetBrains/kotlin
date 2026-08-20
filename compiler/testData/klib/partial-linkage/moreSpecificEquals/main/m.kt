import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(true) { removedEqualityBound() }
    expectSuccess(true) { addedEqualityBound() }
    expectSuccess(false) { differentClasses() }
    expectSuccess(true) { changedEqualityBound() }
}
