import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("referencedFunctionBefore") { getKCallableName() }

    expectSuccess("top-level-v2") { getToConstTrim() }
    expectSuccess("TopLevelV2") { getToConstEnumName() }
    expectSuccess(5u) { getToConstUnsigned() }
    expectSuccess('*') { getToConstCharConstructor() }
    expectSuccess(3.toByte()) { getToValSigned() }

    expectSuccess("object-v2") { getObjectToConstTrimEnd() }
    expectSuccess("ObjectV2") { getObjectToConstEnumName() }
    expectSuccess(12u) { getObjectToConstUnsigned() }
    expectSuccess('*') { getObjectToConstCharConstructor() }
    expectSuccess(2.toShort()) { getObjectToValSigned() }

    expectSuccess("companion-v2") { getCompanionToConstTrimIndent() }
    expectSuccess("CompanionV2") { getCompanionToConstEnumName() }
    expectSuccess(0u) { getCompanionToConstUnsigned() }
    expectSuccess('*') { getCompanionToConstCharConstructor() }
    expectSuccess(0) { getCompanionToValSigned() }

    expectSuccess("companion-object-v2") { getCompanionObjectToConstTrimMargin() }
    expectSuccess("CompanionObjectV2") { getCompanionObjectToConstEnumName() }
    expectSuccess(5u) { getCompanionObjectToConstUnsigned() }
    expectSuccess('*') { getCompanionObjectToConstCharConstructor() }
    expectSuccess(2L) { getCompanionObjectToValSigned() }
}
