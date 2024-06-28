import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(prefixed("???")) { doCommonViaProperty() }
    expectFailure(prefixed("???")) { doSpecificViaProperty() }
    expectFailure(prefixed("???")) { doCommonViaFunction() }
    expectFailure(prefixed("???")) { doSpecificViaFunction() }
}
