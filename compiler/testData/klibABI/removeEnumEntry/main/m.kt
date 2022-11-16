import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(skipHashes("Can not get instance of singleton REMOVED: No enum entry found for symbol /E.REMOVED")) { compute(E.UNCHANGED2) }
    expectFailure(skipHashes("Can not get instance of singleton REMOVED: No enum entry found for symbol /E.REMOVED")) { compute(E.REMOVED) }
    expectSuccess { compute(E.UNCHANGED1) }
}
