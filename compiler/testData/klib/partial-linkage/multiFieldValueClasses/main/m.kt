import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(1) { mfvcToInline() }
    expectSuccess(1) { inlineToMfvc() }

    expectSuccess(1) { oneToTwoParameters() }
    expectSuccess(1) { twoToOneParameters() }

    expectFailure(linkage("Constructor 'E.<init>' can not be called: Can not instantiate abstract value class 'E'")) { mfvcToAbstract() }
    expectFailure(linkage("Constructor 'Derived.<init>' can not be called: Value class 'Derived' inherits from final value class 'F'")) { abstractToMfvc() }

    expectSuccess(1) { classToMfvc() }
    expectSuccess(1) { mfvcToClass() }

    expectSuccess(1) { versionOverloadInlineToMfvc() }
    expectSuccess(1) { versionOverloadMfvc() }
}
