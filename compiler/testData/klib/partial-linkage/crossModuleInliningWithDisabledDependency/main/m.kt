import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineFunction.v2") { InlineFunction.directCall1() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { InlineFunction.directCall2() }
    expectSuccess("inlineFunctionWithParam.v2: directCustomParam") { InlineFunction.directCall3() }
    expectSuccess("directReceiver.inlineExtensionFunction.v2 with context directContext") { InlineFunction.directCall4() }
    expectSuccess("inlineClassFunction.v2") { InlineFunction.directCall5() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { InlineFunction.directCall6() }
    expectSuccess("inlineClassFunctionWithParam.v2: directClassCustomParam") { InlineFunction.directCall7() }
    expectSuccess("directClassReceiver.inlineClassExtensionFunction.v2 with context directClassContext") { InlineFunction.directCall8() }

    expectSuccess("inlineFunction.v2") { InlineFunction.directCall9() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { InlineFunction.directCall10() }
    expectSuccess("inlineFunctionWithParam.v2: inlineCustomParam") { InlineFunction.directCall11() }
    expectSuccess("inlineReceiver.inlineExtensionFunction.v2 with context inlineContext") { InlineFunction.directCall12() }
    expectSuccess("inlineClassFunction.v2") { InlineFunction.directCall13() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { InlineFunction.directCall14() }
    expectSuccess("inlineClassFunctionWithParam.v2: inlineClassCustomParam") { InlineFunction.directCall15() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionFunction.v2 with context inlineClassContext") { InlineFunction.directCall16() }

    expectSuccess("inlineFunction.v2") { InlineFunction.directCall17() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { InlineFunction.directCall18() }
    expectSuccess("inlineFunctionWithParam.v2: lambdaCustomParam") { InlineFunction.directCall19() }
    expectSuccess("lambdaReceiver.inlineExtensionFunction.v2 with context lambdaContext") { InlineFunction.directCall20() }
    expectSuccess("inlineClassFunction.v2") { InlineFunction.directCall21() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { InlineFunction.directCall22() }
    expectSuccess("inlineClassFunctionWithParam.v2: lambdaClassCustomParam") { InlineFunction.directCall23() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionFunction.v2 with context lambdaClassContext") { InlineFunction.directCall24() }

    expectSuccess("inlineFunction.v2") { InlineFunction.directCall25() }
    expectSuccess("inlineFunctionWithParam.v2: default.v2") { InlineFunction.directCall26() }
    expectSuccess("inlineClassFunction.v2") { InlineFunction.directCall27() }
    expectSuccess("inlineClassFunctionWithParam.v2: default.v2") { InlineFunction.directCall28() }

    expectSuccess("inlineProperty.v2") { InlineProperty.directCall1() }
    expectSuccess("directReceiver.inlineExtensionProperty.v2 with context directContext") { InlineProperty.directCall2() }
    expectSuccess("inlineClassProperty.v2") { InlineProperty.directCall3() }
    expectSuccess("directClassReceiver.inlineClassExtensionProperty.v2 with context directClassContext") { InlineProperty.directCall4() }

    expectSuccess("inlineProperty.v2") { InlineProperty.directCall5() }
    expectSuccess("inlineReceiver.inlineExtensionProperty.v2 with context inlineContext") { InlineProperty.directCall6() }
    expectSuccess("inlineClassProperty.v2") { InlineProperty.directCall7() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionProperty.v2 with context inlineClassContext") { InlineProperty.directCall8() }

    expectSuccess("inlineProperty.v2") { InlineProperty.directCall9() }
    expectSuccess("lambdaReceiver.inlineExtensionProperty.v2 with context lambdaContext") { InlineProperty.directCall10() }
    expectSuccess("inlineClassProperty.v2") { InlineProperty.directCall11() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionProperty.v2 with context lambdaClassContext") { InlineProperty.directCall12() }

    expectSuccess("inlineProperty.v2") { InlineProperty.directCall13() }
    expectSuccess("inlineClassProperty.v2") { InlineProperty.directCall14() }
}
