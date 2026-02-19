import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineFunction.v2") { inlineFunction() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { inlineFunctionWithParam() }
    expectSuccess("v2Receiver.inlineExtensionFunction.v2 with context v2Context") { with("v2Context") { "v2Receiver".inlineExtensionFunction() } }
    expectSuccess("inlineClassFunction.v2") { C().inlineClassFunction() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { C().inlineClassFunctionWithParam() }
    expectSuccess("v2ClassReceiver.inlineClassExtensionFunction.v2 with context v2ClassContext") { C().run { with("v2ClassContext") { "v2ClassReceiver".inlineClassExtensionFunction() } } }


    expectSuccess("inlineFunction.v1") { directCall1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { directCall2() }
    expectSuccess("inlineFunctionWithParam.v1: directCustomParam") { directCall3() }
    expectSuccess("directReceiver.inlineExtensionFunction.v1 with context directContext") { directCall4() }
    expectSuccess("inlineClassFunction.v1") { directCall5() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { directCall6() }
    expectSuccess("inlineClassFunctionWithParam.v1: directClassCustomParam") { directCall7() }
    expectSuccess("directClassReceiver.inlineClassExtensionFunction.v1 with context directClassContext") { directCall8() }

    expectSuccess("inlineFunction.v1") { directCall9() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { directCall10() }
    expectSuccess("inlineFunctionWithParam.v1: inlineCustomParam") { directCall11() }
    expectSuccess("inlineReceiver.inlineExtensionFunction.v1 with context inlineContext") { directCall12() }
    expectSuccess("inlineClassFunction.v1") { directCall13() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { directCall14() }
    expectSuccess("inlineClassFunctionWithParam.v1: inlineClassCustomParam") { directCall15() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionFunction.v1 with context inlineClassContext") { directCall16() }

    expectSuccess("inlineFunction.v1") { directCall17() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { directCall18() }
    expectSuccess("inlineFunctionWithParam.v1: lambdaCustomParam") { directCall19() }
    expectSuccess("lambdaReceiver.inlineExtensionFunction.v1 with context lambdaContext") { directCall20() }
    expectSuccess("inlineClassFunction.v1") { directCall21() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { directCall22() }
    expectSuccess("inlineClassFunctionWithParam.v1: lambdaClassCustomParam") { directCall23() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionFunction.v1 with context lambdaClassContext") { directCall24() }

    expectSuccess("inlineFunction.v1") { directCall25() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { directCall26() }
    expectSuccess("inlineClassFunction.v1") { directCall27() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { directCall28() }
}