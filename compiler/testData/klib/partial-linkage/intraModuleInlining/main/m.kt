import abitestutils.abiTest

fun box() = abiTest {
    // full
    expectSuccess("inlineFunctionFull.v2") { InlineFunctionFull.directCall1() }
    expectSuccess("inlineFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall2() }
    expectSuccess("inlineFunctionWithParamFull.v2: directCustomParamFull") { InlineFunctionFull.directCall3() }
    expectSuccess("directReceiverFull.inlineExtensionFunctionFull.v2 with context directContextFull") { InlineFunctionFull.directCall4() }
    expectSuccess("inlineClassFunctionFull.v2") { InlineFunctionFull.directCall5() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall6() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: directClassCustomParamFull") { InlineFunctionFull.directCall7() }
    expectSuccess("directClassReceiverFull.inlineClassExtensionFunctionFull.v2 with context directClassContextFull") { InlineFunctionFull.directCall8() }

    expectSuccess("inlineFunctionFull.v2") { InlineFunctionFull.directCall9() }
    expectSuccess("inlineFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall10() }
    expectSuccess("inlineFunctionWithParamFull.v2: inlineCustomParamFull") { InlineFunctionFull.directCall11() }
    expectSuccess("inlineReceiverFull.inlineExtensionFunctionFull.v2 with context inlineContextFull") { InlineFunctionFull.directCall12() }
    expectSuccess("inlineClassFunctionFull.v2") { InlineFunctionFull.directCall13() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall14() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: inlineClassCustomParamFull") { InlineFunctionFull.directCall15() }
    expectSuccess("inlineClassReceiverFull.inlineClassExtensionFunctionFull.v2 with context inlineClassContextFull") { InlineFunctionFull.directCall16() }

    expectSuccess("inlineFunctionFull.v2") { InlineFunctionFull.directCall17() }
    expectSuccess("inlineFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall18() }
    expectSuccess("inlineFunctionWithParamFull.v2: lambdaCustomParamFull") { InlineFunctionFull.directCall19() }
    expectSuccess("lambdaReceiverFull.inlineExtensionFunctionFull.v2 with context lambdaContextFull") { InlineFunctionFull.directCall20() }
    expectSuccess("inlineClassFunctionFull.v2") { InlineFunctionFull.directCall21() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall22() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: lambdaClassCustomParamFull") { InlineFunctionFull.directCall23() }
    expectSuccess("lambdaClassReceiverFull.inlineClassExtensionFunctionFull.v2 with context lambdaClassContextFull") { InlineFunctionFull.directCall24() }

    expectSuccess("inlineFunctionFull.v2") { InlineFunctionFull.directCall25() }
    expectSuccess("inlineFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall26() }
    expectSuccess("inlineClassFunctionFull.v2") { InlineFunctionFull.directCall27() }
    expectSuccess("inlineClassFunctionWithParamFull.v2: defaultFull.v2") { InlineFunctionFull.directCall28() }

    expectSuccess("inlinePropertyFull.v2") { InlinePropertyFull.directCall1() }
    expectSuccess("directReceiverFull.inlineExtensionPropertyFull.v2 with context directContextFull") { InlinePropertyFull.directCall2() }
    expectSuccess("inlineClassPropertyFull.v2") { InlinePropertyFull.directCall3() }
    expectSuccess("directClassReceiverFull.inlineClassExtensionPropertyFull.v2 with context directClassContextFull") { InlinePropertyFull.directCall4() }

    expectSuccess("inlinePropertyFull.v2") { InlinePropertyFull.directCall5() }
    expectSuccess("inlineReceiverFull.inlineExtensionPropertyFull.v2 with context inlineContextFull") { InlinePropertyFull.directCall6() }
    expectSuccess("inlineClassPropertyFull.v2") { InlinePropertyFull.directCall7() }
    expectSuccess("inlineClassReceiverFull.inlineClassExtensionPropertyFull.v2 with context inlineClassContextFull") { InlinePropertyFull.directCall8() }

    expectSuccess("inlinePropertyFull.v2") { InlinePropertyFull.directCall9() }
    expectSuccess("lambdaReceiverFull.inlineExtensionPropertyFull.v2 with context lambdaContextFull") { InlinePropertyFull.directCall10() }
    expectSuccess("inlineClassPropertyFull.v2") { InlinePropertyFull.directCall11() }
    expectSuccess("lambdaClassReceiverFull.inlineClassExtensionPropertyFull.v2 with context lambdaClassContextFull") { InlinePropertyFull.directCall12() }

    expectSuccess("inlinePropertyFull.v2") { InlinePropertyFull.directCall13() }
    expectSuccess("inlineClassPropertyFull.v2") { InlinePropertyFull.directCall14() }

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

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall1() }
    expectSuccess("directReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context directContextIntraModule") { InlinePropertyIntraModule.directCall2() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall3() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context directClassContextIntraModule") { InlinePropertyIntraModule.directCall4() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall5() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context inlineContextIntraModule") { InlinePropertyIntraModule.directCall6() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall7() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context inlineClassContextIntraModule") { InlinePropertyIntraModule.directCall8() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall9() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context lambdaContextIntraModule") { InlinePropertyIntraModule.directCall10() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall11() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context lambdaClassContextIntraModule") { InlinePropertyIntraModule.directCall12() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall13() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall14() }

    expectSuccess("inlineFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall1() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall2() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: directCustomParamIntraModule") { InlineFunctionIntraModule.directCall3() }
    expectSuccess("directReceiverIntraModule.inlineExtensionFunctionIntraModule.v2 with context directContextIntraModule") { InlineFunctionIntraModule.directCall4() }
    expectSuccess("inlineClassFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall5() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall6() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: directClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall7() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v2 with context directClassContextIntraModule") { InlineFunctionIntraModule.directCall8() }

    expectSuccess("inlineFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall9() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall10() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: inlineCustomParamIntraModule") { InlineFunctionIntraModule.directCall11() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionFunctionIntraModule.v2 with context inlineContextIntraModule") { InlineFunctionIntraModule.directCall12() }
    expectSuccess("inlineClassFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall13() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall14() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: inlineClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall15() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v2 with context inlineClassContextIntraModule") { InlineFunctionIntraModule.directCall16() }

    expectSuccess("inlineFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall17() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall18() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: lambdaCustomParamIntraModule") { InlineFunctionIntraModule.directCall19() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionFunctionIntraModule.v2 with context lambdaContextIntraModule") { InlineFunctionIntraModule.directCall20() }
    expectSuccess("inlineClassFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall21() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall22() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: lambdaClassCustomParamIntraModule") { InlineFunctionIntraModule.directCall23() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionFunctionIntraModule.v2 with context lambdaClassContextIntraModule") { InlineFunctionIntraModule.directCall24() }

    expectSuccess("inlineFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall25() }
    expectSuccess("inlineFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall26() }
    expectSuccess("inlineClassFunctionIntraModule.v2") { InlineFunctionIntraModule.directCall27() }
    expectSuccess("inlineClassFunctionWithParamIntraModule.v2: defaultIntraModule.v2") { InlineFunctionIntraModule.directCall28() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall1() }
    expectSuccess("directReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context directContextIntraModule") { InlinePropertyIntraModule.directCall2() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall3() }
    expectSuccess("directClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context directClassContextIntraModule") { InlinePropertyIntraModule.directCall4() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall5() }
    expectSuccess("inlineReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context inlineContextIntraModule") { InlinePropertyIntraModule.directCall6() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall7() }
    expectSuccess("inlineClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context inlineClassContextIntraModule") { InlinePropertyIntraModule.directCall8() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall9() }
    expectSuccess("lambdaReceiverIntraModule.inlineExtensionPropertyIntraModule.v2 with context lambdaContextIntraModule") { InlinePropertyIntraModule.directCall10() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall11() }
    expectSuccess("lambdaClassReceiverIntraModule.inlineClassExtensionPropertyIntraModule.v2 with context lambdaClassContextIntraModule") { InlinePropertyIntraModule.directCall12() }

    expectSuccess("inlinePropertyIntraModule.v2") { InlinePropertyIntraModule.directCall13() }
    expectSuccess("inlineClassPropertyIntraModule.v2") { InlinePropertyIntraModule.directCall14() }
}