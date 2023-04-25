import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Can not get instance of singleton 'E.REMOVED': No enum entry found for symbol '/E.REMOVED'")) { compute(E.UNCHANGED2) }
    expectFailure(linkage("Can not get instance of singleton 'E.REMOVED': No enum entry found for symbol '/E.REMOVED'")) { compute(E.REMOVED) }
    expectSuccess { compute(E.UNCHANGED1) }
}
