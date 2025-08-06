import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineProperty.v2") { inlineProperty }
    expectSuccess("receiver.inlineExtensionProperty.v2 with context c") { with("c") { "receiver".inlineExtensionProperty } }
    expectSuccess("inlineClassProperty.v2") { C().inlineClassProperty }
    expectSuccess("receiver.inlineClassExtensionProperty.v2 with context c") { C().run { with("c") { "receiver".inlineClassExtensionProperty } } }


    expectSuccess("inlineProperty.v1") { directCall1() }
    expectSuccess("receiver.inlineExtensionProperty.v1 with context c") { directCall2() }
    expectSuccess("inlineClassProperty.v1") { directCall3() }
    expectSuccess("receiver.inlineClassExtensionProperty.v1 with context c") { directCall4() }

    expectSuccess("inlineProperty.v1") { inlineCall1() }
    expectSuccess("receiver.inlineExtensionProperty.v1 with context c") { inlineCall2() }
    expectSuccess("inlineClassProperty.v1") { inlineCall3() }
    expectSuccess("receiver.inlineClassExtensionProperty.v1 with context c") { inlineCall4() }

    expectSuccess("inlineProperty.v1") { lambdaCall1() }
    expectSuccess("receiver.inlineExtensionProperty.v1 with context c") { lambdaCall2() }
    expectSuccess("inlineClassProperty.v1") { lambdaCall3() }
    expectSuccess("receiver.inlineClassExtensionProperty.v1 with context c") { lambdaCall4() }

    expectSuccess("inlineProperty.v1") { defaultParamFunction1() }
    expectSuccess("inlineClassProperty.v1") { defaultParamFunction2() }
}