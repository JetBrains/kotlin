import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Must not throw IrLinkageError")) {
        compute(E.ADDED)
    }
}
