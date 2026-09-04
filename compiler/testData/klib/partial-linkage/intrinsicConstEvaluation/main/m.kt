import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("referencedFunctionBefore") { getKCallableName() }

    expectSuccess("top-level-v2") { getToConstTrim() }
    expectSuccess("TopLevelV2") { getToConstEnumName() }
    expectSuccess(5u) { getToConstUnsignedOperation() }
    expectSuccess('*') { getToConstCharConstructor() }

    expectSuccess("object-v2") { getObjectToConstTrimEnd() }
    expectSuccess("ObjectV2") { getObjectToConstEnumName() }
    expectSuccess(12u) { getObjectToConstUnsignedOperation() }
    expectSuccess('*') { getObjectToConstCharConstructor() }

    expectSuccess("companion-v2") { getCompanionToConstTrimIndent() }
    expectSuccess("CompanionV2") { getCompanionToConstEnumName() }
    expectSuccess(0u) { getCompanionToConstUnsignedOperation() }
    expectSuccess('*') { getCompanionToConstCharConstructor() }

    expectSuccess("companion-object-v2") { getCompanionObjectToConstTrimMargin() }
    expectSuccess("CompanionObjectV2") { getCompanionObjectToConstEnumName() }
    expectSuccess(5u) { getCompanionObjectToConstUnsignedOperation() }
    expectSuccess('*') { getCompanionObjectToConstCharConstructor() }
}
