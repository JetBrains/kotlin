import abitestutils.abiTest

fun box() = abiTest {
    // full
    expectSuccess("inlineFunctionFull.v1") { InlineFunctionFull.directCall1() }
    expectSuccess("inlineFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall2() }
    expectSuccess("inlineFunctionWithParamFull.v1: directCustomParamFull") { InlineFunctionFull.directCall3() }
    expectSuccess("directReceiverFull.inlineExtensionFunctionFull.v1 with context directContextFull") { InlineFunctionFull.directCall4() }
    expectSuccess("inlineClassFunctionFull.v1") { InlineFunctionFull.directCall5() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall6() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: directClassCustomParamFull") { InlineFunctionFull.directCall7() }
    expectSuccess("directClassReceiverFull.inlineClassExtensionFunctionFull.v1 with context directClassContextFull") { InlineFunctionFull.directCall8() }

    expectSuccess("inlineFunctionFull.v1") { InlineFunctionFull.directCall9() }
    expectSuccess("inlineFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall10() }
    expectSuccess("inlineFunctionWithParamFull.v1: inlineCustomParamFull") { InlineFunctionFull.directCall11() }
    expectSuccess("inlineReceiverFull.inlineExtensionFunctionFull.v1 with context inlineContextFull") { InlineFunctionFull.directCall12() }
    expectSuccess("inlineClassFunctionFull.v1") { InlineFunctionFull.directCall13() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall14() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: inlineClassCustomParamFull") { InlineFunctionFull.directCall15() }
    expectSuccess("inlineClassReceiverFull.inlineClassExtensionFunctionFull.v1 with context inlineClassContextFull") { InlineFunctionFull.directCall16() }

    expectSuccess("inlineFunctionFull.v1") { InlineFunctionFull.directCall17() }
    expectSuccess("inlineFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall18() }
    expectSuccess("inlineFunctionWithParamFull.v1: lambdaCustomParamFull") { InlineFunctionFull.directCall19() }
    expectSuccess("lambdaReceiverFull.inlineExtensionFunctionFull.v1 with context lambdaContextFull") { InlineFunctionFull.directCall20() }
    expectSuccess("inlineClassFunctionFull.v1") { InlineFunctionFull.directCall21() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall22() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: lambdaClassCustomParamFull") { InlineFunctionFull.directCall23() }
    expectSuccess("lambdaClassReceiverFull.inlineClassExtensionFunctionFull.v1 with context lambdaClassContextFull") { InlineFunctionFull.directCall24() }

    expectSuccess("inlineFunctionFull.v1") { InlineFunctionFull.directCall25() }
    expectSuccess("inlineFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall26() }
    expectSuccess("inlineClassFunctionFull.v1") { InlineFunctionFull.directCall27() }
    expectSuccess("inlineClassFunctionWithParamFull.v1: defaultFull.v1") { InlineFunctionFull.directCall28() }

    expectSuccess("inlinePropertyFull.v1") { InlinePropertyFull.directCall1() }
    expectSuccess("directReceiverFull.inlineExtensionPropertyFull.v1 with context directContextFull") { InlinePropertyFull.directCall2() }
    expectSuccess("inlineClassPropertyFull.v1") { InlinePropertyFull.directCall3() }
    expectSuccess("directClassReceiverFull.inlineClassExtensionPropertyFull.v1 with context directClassContextFull") { InlinePropertyFull.directCall4() }

    expectSuccess("inlinePropertyFull.v1") { InlinePropertyFull.directCall5() }
    expectSuccess("inlineReceiverFull.inlineExtensionPropertyFull.v1 with context inlineContextFull") { InlinePropertyFull.directCall6() }
    expectSuccess("inlineClassPropertyFull.v1") { InlinePropertyFull.directCall7() }
    expectSuccess("inlineClassReceiverFull.inlineClassExtensionPropertyFull.v1 with context inlineClassContextFull") { InlinePropertyFull.directCall8() }

    expectSuccess("inlinePropertyFull.v1") { InlinePropertyFull.directCall9() }
    expectSuccess("lambdaReceiverFull.inlineExtensionPropertyFull.v1 with context lambdaContextFull") { InlinePropertyFull.directCall10() }
    expectSuccess("inlineClassPropertyFull.v1") { InlinePropertyFull.directCall11() }
    expectSuccess("lambdaClassReceiverFull.inlineClassExtensionPropertyFull.v1 with context lambdaClassContextFull") { InlinePropertyFull.directCall12() }

    expectSuccess("inlinePropertyFull.v1") { InlinePropertyFull.directCall13() }
    expectSuccess("inlineClassPropertyFull.v1") { InlinePropertyFull.directCall14() }

    // disabled

    expectSuccess("inlineFunctionDisabled.v2") { InlineFunctionDisabled.directCall1() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall2() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: directCustomParamDisabled") { InlineFunctionDisabled.directCall3() }
    expectSuccess("directReceiverDisabled.inlineExtensionFunctionDisabled.v2 with context directContextDisabled") { InlineFunctionDisabled.directCall4() }
    expectSuccess("inlineClassFunctionDisabled.v2") { InlineFunctionDisabled.directCall5() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall6() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: directClassCustomParamDisabled") { InlineFunctionDisabled.directCall7() }
    expectSuccess("directClassReceiverDisabled.inlineClassExtensionFunctionDisabled.v2 with context directClassContextDisabled") { InlineFunctionDisabled.directCall8() }

    expectSuccess("inlineFunctionDisabled.v2") { InlineFunctionDisabled.directCall9() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall10() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: inlineCustomParamDisabled") { InlineFunctionDisabled.directCall11() }
    expectSuccess("inlineReceiverDisabled.inlineExtensionFunctionDisabled.v2 with context inlineContextDisabled") { InlineFunctionDisabled.directCall12() }
    expectSuccess("inlineClassFunctionDisabled.v2") { InlineFunctionDisabled.directCall13() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall14() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: inlineClassCustomParamDisabled") { InlineFunctionDisabled.directCall15() }
    expectSuccess("inlineClassReceiverDisabled.inlineClassExtensionFunctionDisabled.v2 with context inlineClassContextDisabled") { InlineFunctionDisabled.directCall16() }

    expectSuccess("inlineFunctionDisabled.v2") { InlineFunctionDisabled.directCall17() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall18() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: lambdaCustomParamDisabled") { InlineFunctionDisabled.directCall19() }
    expectSuccess("lambdaReceiverDisabled.inlineExtensionFunctionDisabled.v2 with context lambdaContextDisabled") { InlineFunctionDisabled.directCall20() }
    expectSuccess("inlineClassFunctionDisabled.v2") { InlineFunctionDisabled.directCall21() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall22() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: lambdaClassCustomParamDisabled") { InlineFunctionDisabled.directCall23() }
    expectSuccess("lambdaClassReceiverDisabled.inlineClassExtensionFunctionDisabled.v2 with context lambdaClassContextDisabled") { InlineFunctionDisabled.directCall24() }

    expectSuccess("inlineFunctionDisabled.v2") { InlineFunctionDisabled.directCall25() }
    expectSuccess("inlineFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall26() }
    expectSuccess("inlineClassFunctionDisabled.v2") { InlineFunctionDisabled.directCall27() }
    expectSuccess("inlineClassFunctionWithParamDisabled.v2: defaultDisabled.v2") { InlineFunctionDisabled.directCall28() }
    
    // intra-module

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall1() }
    expectSuccess("directReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context directContextIntraModule") { InlinePropertyIntraModule.directCall2() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall3() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context directClassContextIntraModule") { InlinePropertyIntraModule.directCall4() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall5() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context inlineContextIntraModule") { InlinePropertyIntraModule.directCall6() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall7() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context inlineClassContextIntraModule") { InlinePropertyIntraModule.directCall8() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall9() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context lambdaContextIntraModule") { InlinePropertyIntraModule.directCall10() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall11() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context lambdaClassContextIntraModule") { InlinePropertyIntraModule.directCall12() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall13() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall14() }

    expectSuccess("inlineFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall1() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall2() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: directCustomParamIntraModule") { InlineFunctionIntraModule.directCall3() }
    expectSuccess("directReceiverIntraModule.inlineExtensionFunctionIntraModule.v1 with context directContextIntraModule") { InlineFunctionIntraModule.directCall4() }
    expectSuccess("inlineClassFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall5() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall6() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: directClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall7() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v1 with context directClassContextIntraModule") { InlineFunctionIntraModule.directCall8() }

    expectSuccess("inlineFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall9() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall10() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: inlineCustomParamIntraModule") { InlineFunctionIntraModule.directCall11() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionFunctionIntraModule.v1 with context inlineContextIntraModule") { InlineFunctionIntraModule.directCall12() }
    expectSuccess("inlineClassFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall13() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall14() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: inlineClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall15() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v1 with context inlineClassContextIntraModule") { InlineFunctionIntraModule.directCall16() }

    expectSuccess("inlineFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall17() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall18() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: lambdaCustomParamIntraModule") { InlineFunctionIntraModule.directCall19() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionFunctionIntraModule.v1 with context lambdaContextIntraModule") { InlineFunctionIntraModule.directCall20() }
    expectSuccess("inlineClassFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall21() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall22() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: lambdaClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall23() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v1 with context lambdaClassContextIntraModule") { InlineFunctionIntraModule.directCall24() }

    expectSuccess("inlineFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall25() }
    expectSuccess("inlineFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall26() }
    expectSuccess("inlineClassFunctionIntraModule.v1") { InlineFunctionIntraModule.directCall27() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v1: defaultIntraModule.v1") { InlineFunctionIntraModule.directCall28() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall1() }
    expectSuccess("directReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context directContextIntraModule") { InlinePropertyIntraModule.directCall2() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall3() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context directClassContextIntraModule") { InlinePropertyIntraModule.directCall4() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall5() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context inlineContextIntraModule") { InlinePropertyIntraModule.directCall6() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall7() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context inlineClassContextIntraModule") { InlinePropertyIntraModule.directCall8() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall9() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionPropertyIntraModule.v1 with context lambdaContextIntraModule") { InlinePropertyIntraModule.directCall10() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall11() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v1 with context lambdaClassContextIntraModule") { InlinePropertyIntraModule.directCall12() }

    expectSuccess("inlinePropertyIntraModule.v1") { InlinePropertyIntraModule.directCall13() }
    expectSuccess("inlineClassPropertyIntraModule.v1") { InlinePropertyIntraModule.directCall14() }
}