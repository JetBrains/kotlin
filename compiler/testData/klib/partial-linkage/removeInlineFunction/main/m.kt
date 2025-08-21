import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("topLevelInlineFunction") { directCall1() }
    expectSuccess("topLevelInlineFunctionWithParam directParamValue") { directCall2() }
    expectSuccess("directReceiverValue.topLevelInlineFunctionWithReceiver with context directContextValue") { directCall3() }
    expectSuccess("classlInlineFunction") { directCall4() }
    expectSuccess("classlInlineFunctionWithParam directClassParamValue") { directCall5() }
    expectSuccess("directClassReceiverValue.classlInlineFunctionWithReceiver with context directClassContextValue") { directCall6() }

    expectSuccess("topLevelInlineFunction") { directCall7() }
    expectSuccess("topLevelInlineFunctionWithParam inlineParamValue") { directCall8() }
    expectSuccess("inlineReceiverValue.topLevelInlineFunctionWithReceiver with context inlineContextValue") { directCall9() }
    expectSuccess("classlInlineFunction") { directCall10() }
    expectSuccess("classlInlineFunctionWithParam inlineClassParamValue") { directCall11() }
    expectSuccess("inlineClassReceiverValue.classlInlineFunctionWithReceiver with context inlineClassContextValue") { directCall12() }

    expectSuccess("topLevelInlineFunction") { directCall13() }
    expectSuccess("topLevelInlineFunctionWithParam lambdaParamValue") { directCall14() }
    expectSuccess("lambdaReceiverValue.topLevelInlineFunctionWithReceiver with context lambdaContextValue") { directCall15() }
    expectSuccess("classlInlineFunction") { directCall16() }
    expectSuccess("classlInlineFunctionWithParam lambdaClassParamValue") { directCall17() }
    expectSuccess("lambdaClassReceiverValue.classlInlineFunctionWithReceiver with context lambdaClassContextValue") { directCall18() }
}