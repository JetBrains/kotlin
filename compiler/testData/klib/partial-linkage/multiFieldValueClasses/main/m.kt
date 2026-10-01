import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess(1) { mfvcToInline() }
    expectSuccess(1) { inlineToMfvc() }

    expectSuccess(1) { oneToTwoParameters() }
    expectSuccess(1) { twoToOneParameters1() }
    expectFailure(linkage("Property accessor 'second.<get-second>' can not be called: No property accessor found for symbol '/TwoToOneParams.second.<get-second>'")) { twoToOneParameters2() }

    expectFailure(linkage("Constructor 'MfvcToAbstract.<init>' can not be called: Can not instantiate abstract value class 'MfvcToAbstract'")) { mfvcToAbstract() }
    expectFailure(linkage("Constructor 'AbstractToMfvcSubclass.<init>' can not be called: Value class 'AbstractToMfvcSubclass' inherits from final value class 'AbstractToMfvc'")) { abstractToMfvc() }

    expectSuccess(1) { classToMfvc1() }
    expectSuccess(2) { classToMfvc2() }
    expectSuccess(1) { mfvcToClass1() }
    expectSuccess(2) { mfvcToClass2() }

    expectSuccess(1) { valueToIdentity() }
    expectSuccess(1) { staysValue() }

    expectSuccess(1) { inlineToMfvcVersionOverload() }
    expectSuccess(1) { mfvcVersionOverload() }
}
