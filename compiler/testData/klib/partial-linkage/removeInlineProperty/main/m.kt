import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("topLevelProperty") { getterDirectCall1() }
    expectSuccess("topLevelPropertyWithReceiver") { getterDirectCall2() }
    expectSuccess("classProperty") { getterDirectCall3() }
    expectSuccess("classPropertyWithReceiver") { getterDirectCall4() }

    expectSuccess("topLevelProperty") { getterDirectCall5() }
    expectSuccess("topLevelPropertyWithReceiver") { getterDirectCall6() }
    expectSuccess("classProperty") { getterDirectCall7() }
    expectSuccess("classPropertyWithReceiver") { getterDirectCall8() }

    expectSuccess("topLevelProperty") { getterDirectCall9() }
    expectSuccess("topLevelPropertyWithReceiver") { getterDirectCall10() }
    expectSuccess("classProperty") { getterDirectCall11() }
    expectSuccess("classPropertyWithReceiver") { getterDirectCall12() }


    expectSuccess("directSetterValue") { setterDirectCall1() }
    expectSuccess("directReceiverValue.directSetterValue with context directContextValue") { setterDirectCall2() }
    expectSuccess("directClassSetterValue") { setterDirectCall3() }
    expectSuccess("directClassReceiverValue.directClassSetterValue with context directClassContextValue") { setterDirectCall4() }

    expectSuccess("inlineSetterValue") { setterDirectCall5() }
    expectSuccess("inlineReceiverValue.inlineSetterValue with context inlineContextValue") { setterDirectCall6() }
    expectSuccess("inlineClassSetterValue") { setterDirectCall7() }
    expectSuccess("inlineClassReceiverValue.inlineClassSetterValue with context inlineClassContextValue") { setterDirectCall8() }

    expectSuccess("lambdaSetterValue") { setterDirectCall9() }
    expectSuccess("lambdaReceiverValue.lambdaSetterValue with context lambdaContextValue") { setterDirectCall10() }
    expectSuccess("lambdaClassSetterValue") { setterDirectCall11() }
    expectSuccess("lambdaClassReceiverValue.lambdaClassSetterValue with context lambdaClassContextValue") { setterDirectCall12() }
}