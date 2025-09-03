import abitestutils.abiTest

fun box() = abiTest {
    // Property annotated
    expectSuccess("directSetterValue.v2") { PropertyAnnotated.directCall1() }
    expectSuccess("directReceiver.directSetterValue.v2 with context directContext") { PropertyAnnotated.directCall2() }
    expectSuccess("directSetterValue.v2") { PropertyAnnotated.directCall3() }
    expectSuccess("directClassReceiver.directSetterValue.v2 with context directClassContext") { PropertyAnnotated.directCall4() }

    expectSuccess("inlineSetterValue.v2") { PropertyAnnotated.directCall5() }
    expectSuccess("inlineReceiver.inlineSetterValue.v2 with context inlineContext") { PropertyAnnotated.directCall6() }
    expectSuccess("inlineSetterValue.v2") { PropertyAnnotated.directCall7() }
    expectSuccess("inlineClassReceiver.inlineSetterValue.v2 with context inlineClassContext") { PropertyAnnotated.directCall8() }

    expectSuccess("lambdaSetterValue.v2") { PropertyAnnotated.directCall9() }
    expectSuccess("lambdaReceiver.lambdaSetterValue.v2 with context lambdaContext") { PropertyAnnotated.directCall10() }
    expectSuccess("lambdaSetterValue.v2") { PropertyAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiver.lambdaSetterValue.v2 with context lambdaClassContext") { PropertyAnnotated.directCall12() }

    // Setter annotated
    expectSuccess("directSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall1() }
    expectSuccess("directReceiverAnnotatedSetter.directSetterValueAnnotatedSetter.v2 with context directContextAnnotatedSetter") { SetterAnnotated.directCall2() }
    expectSuccess("directSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall3() }
    expectSuccess("directClassReceiverAnnotatedSetter.directSetterValueAnnotatedSetter.v2 with context directClassContextAnnotatedSetter") { SetterAnnotated.directCall4() }

    expectSuccess("inlineSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall5() }
    expectSuccess("inlineReceiverAnnotatedSetter.inlineSetterValueAnnotatedSetter.v2 with context inlineContextAnnotatedSetter") { SetterAnnotated.directCall6() }
    expectSuccess("inlineSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall7() }
    expectSuccess("inlineClassReceiverAnnotatedSetter.inlineSetterValueAnnotatedSetter.v2 with context inlineClassContextAnnotatedSetter") { SetterAnnotated.directCall8() }

    expectSuccess("lambdaSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall9() }
    expectSuccess("lambdaReceiverAnnotatedSetter.lambdaSetterValueAnnotatedSetter.v2 with context lambdaContextAnnotatedSetter") { SetterAnnotated.directCall10() }
    expectSuccess("lambdaSetterValueAnnotatedSetter.v2") { SetterAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiverAnnotatedSetter.lambdaSetterValueAnnotatedSetter.v2 with context lambdaClassContextAnnotatedSetter") { SetterAnnotated.directCall12() }
}