import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineFunction.v2") { directCall1() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { directCall2() }
    expectSuccess("inlineFunctionWithParam.v2: directCustomParam") { directCall3() }
    expectSuccess("directReceiver.inlineExtensionFunction.v2 with context directContext") { directCall4() }
    expectSuccess("inlineClassFunction.v2") { directCall5() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { directCall6() }
    expectSuccess("inlineClassFunctionWithParam.v2: directClassCustomParam") { directCall7() }
    expectSuccess("directClassReceiver.inlineClassExtensionFunction.v2 with context directClassContext") { directCall8() }

    expectSuccess("inlineFunction.v2") { directCall9() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { directCall10() }
    expectSuccess("inlineFunctionWithParam.v2: inlineCustomParam") { directCall11() }
    expectSuccess("inlineReceiver.inlineExtensionFunction.v2 with context inlineContext") { directCall12() }
    expectSuccess("inlineClassFunction.v2") { directCall13() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { directCall14() }
    expectSuccess("inlineClassFunctionWithParam.v2: inlineClassCustomParam") { directCall15() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionFunction.v2 with context inlineClassContext") { directCall16() }

    expectSuccess("inlineFunction.v2") { directCall17() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { directCall18() }
    expectSuccess("inlineFunctionWithParam.v2: lambdaCustomParam") { directCall19() }
    expectSuccess("lambdaReceiver.inlineExtensionFunction.v2 with context lambdaContext") { directCall20() }
    expectSuccess("inlineClassFunction.v2") { directCall21() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { directCall22() }
    expectSuccess("inlineClassFunctionWithParam.v2: lambdaClassCustomParam") { directCall23() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionFunction.v2 with context lambdaClassContext") { directCall24() }

    expectSuccess("inlineFunction.v2") { directCall25() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { directCall26() }
    expectSuccess("inlineClassFunction.v2") { directCall27() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { directCall28() }
}