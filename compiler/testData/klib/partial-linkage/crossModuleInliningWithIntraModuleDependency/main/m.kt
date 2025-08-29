import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("inlineFunction.v1") { InlineFunction.directCall1() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { InlineFunction.directCall2() }
    expectSuccess("inlineFunctionWithParam.v1: directCustomParam") { InlineFunction.directCall3() }
    expectSuccess("directReceiver.inlineExtensionFunction.v1 with context directContext") { InlineFunction.directCall4() }
    expectSuccess("inlineClassFunction.v1") { InlineFunction.directCall5() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { InlineFunction.directCall6() }
    expectSuccess("inlineClassFunctionWithParam.v1: directClassCustomParam") { InlineFunction.directCall7() }
    expectSuccess("directClassReceiver.inlineClassExtensionFunction.v1 with context directClassContext") { InlineFunction.directCall8() }

    expectSuccess("inlineFunction.v1") { InlineFunction.directCall9() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { InlineFunction.directCall10() }
    expectSuccess("inlineFunctionWithParam.v1: inlineCustomParam") { InlineFunction.directCall11() }
    expectSuccess("inlineReceiver.inlineExtensionFunction.v1 with context inlineContext") { InlineFunction.directCall12() }
    expectSuccess("inlineClassFunction.v1") { InlineFunction.directCall13() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { InlineFunction.directCall14() }
    expectSuccess("inlineClassFunctionWithParam.v1: inlineClassCustomParam") { InlineFunction.directCall15() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionFunction.v1 with context inlineClassContext") { InlineFunction.directCall16() }

    expectSuccess("inlineFunction.v1") { InlineFunction.directCall17() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { InlineFunction.directCall18() }
    expectSuccess("inlineFunctionWithParam.v1: lambdaCustomParam") { InlineFunction.directCall19() }
    expectSuccess("lambdaReceiver.inlineExtensionFunction.v1 with context lambdaContext") { InlineFunction.directCall20() }
    expectSuccess("inlineClassFunction.v1") { InlineFunction.directCall21() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { InlineFunction.directCall22() }
    expectSuccess("inlineClassFunctionWithParam.v1: lambdaClassCustomParam") { InlineFunction.directCall23() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionFunction.v1 with context lambdaClassContext") { InlineFunction.directCall24() }

    expectSuccess("inlineFunction.v1") { InlineFunction.directCall25() }
    expectSuccess("inlineFunctionWithParam.v1: default.v1") { InlineFunction.directCall26() }
    expectSuccess("inlineClassFunction.v1") { InlineFunction.directCall27() }
    expectSuccess("inlineClassFunctionWithParam.v1: default.v1") { InlineFunction.directCall28() }

    expectSuccess("inlineProperty.v1") { InlineProperty.directCall1() }
    expectSuccess("directReceiver.inlineExtensionProperty.v1 with context directContext") { InlineProperty.directCall2() }
    expectSuccess("inlineClassProperty.v1") { InlineProperty.directCall3() }
    expectSuccess("directClassReceiver.inlineClassExtensionProperty.v1 with context directClassContext") { InlineProperty.directCall4() }

    expectSuccess("inlineProperty.v1") { InlineProperty.directCall5() }
    expectSuccess("inlineReceiver.inlineExtensionProperty.v1 with context inlineContext") { InlineProperty.directCall6() }
    expectSuccess("inlineClassProperty.v1") { InlineProperty.directCall7() }
    expectSuccess("inlineClassReceiver.inlineClassExtensionProperty.v1 with context inlineClassContext") { InlineProperty.directCall8() }

    expectSuccess("inlineProperty.v1") { InlineProperty.directCall9() }
    expectSuccess("lambdaReceiver.inlineExtensionProperty.v1 with context lambdaContext") { InlineProperty.directCall10() }
    expectSuccess("inlineClassProperty.v1") { InlineProperty.directCall11() }
    expectSuccess("lambdaClassReceiver.inlineClassExtensionProperty.v1 with context lambdaClassContext") { InlineProperty.directCall12() }

    expectSuccess("inlineProperty.v1") { InlineProperty.directCall13() }
    expectSuccess("inlineClassProperty.v1") { InlineProperty.directCall14() }
}