import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineProperty.v2") { inlineProperty }
    expectSuccess("receiver.inlineExtensionProperty.v2 with context c") { with("c") { "receiver".inlineExtensionProperty } }
    expectSuccess("inlineClassProperty.v2") { C().inlineClassProperty }
    expectSuccess("receiver.inlineClassExtensionProperty.v2 with context c") { C().run { with("c") { "receiver".inlineClassExtensionProperty } } }


    expectSuccess("inlineProperty.v1") { directCall1() }
    expectSuccess("directReceiver.inlineExtensionProperty.v1 with context directContext") { directCall2() }
    expectSuccess("inlineClassProperty.v1") { directCall3() }
    expectSuccess("directClassReceiver.inlineClassExtensionProperty.v1 with context directClassContext") { directCall4() }

    expectSuccess("inlineProperty.v1") { directCall5() }
    expectSuccess("inlineReceiver.inlineExtensionProperty.v1 with context inlineContext") { directCall6() }
    expectSuccess("inlineClassProperty.v1") { directCall7() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionProperty.v1 with context inlineClassContext") { directCall8() }

    expectSuccess("inlineProperty.v1") { directCall9() }
    expectSuccess("lambdaReceiver.inlineExtensionProperty.v1 with context lambdaContext") { directCall10() }
    expectSuccess("inlineClassProperty.v1") { directCall11() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionProperty.v1 with context lambdaClassContext") { directCall12() }

    expectSuccess("inlineProperty.v1") { directCall13() }
    expectSuccess("inlineClassProperty.v1") { directCall14() }
}