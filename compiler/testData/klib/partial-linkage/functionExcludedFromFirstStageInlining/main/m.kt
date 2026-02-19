import abitestutils.abiTest

fun box() = abiTest {
    // Function annotated:
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

    // Read-only property getter annotated:
    expectSuccess("excludedReadOnlyInlinePropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall1() }
    expectSuccess("directReceiver.excludedReadOnlyInlineExtensionPropertyGetter.v2 with context directContext") { ReadOnlyGetterAnnotated.directCall2() }
    expectSuccess("excludedReadOnlyInlineClassPropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall3() }
    expectSuccess("directClassReceiver.excludedReadOnlyInlineClassExtensionPropertyGetter.v2 with context directClassContext") { ReadOnlyGetterAnnotated.directCall4() }

    expectSuccess("excludedReadOnlyInlinePropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall5() }
    expectSuccess("inlineReceiver.excludedReadOnlyInlineExtensionPropertyGetter.v2 with context inlineContext") { ReadOnlyGetterAnnotated.directCall6() }
    expectSuccess("excludedReadOnlyInlineClassPropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall7() }
    expectSuccess("inlineClassReceiver.excludedReadOnlyInlineClassExtensionPropertyGetter.v2 with context inlineClassContext") { ReadOnlyGetterAnnotated.directCall8() }

    expectSuccess("excludedReadOnlyInlinePropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall9() }
    expectSuccess("lambdaReceiver.excludedReadOnlyInlineExtensionPropertyGetter.v2 with context lambdaContext") { ReadOnlyGetterAnnotated.directCall10() }
    expectSuccess("excludedReadOnlyInlineClassPropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiver.excludedReadOnlyInlineClassExtensionPropertyGetter.v2 with context lambdaClassContext") { ReadOnlyGetterAnnotated.directCall12() }

    expectSuccess("excludedReadOnlyInlinePropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall13() }
    expectSuccess("excludedReadOnlyInlineClassPropertyGetter.v2") { ReadOnlyGetterAnnotated.directCall14() }

    // Read-write property setter annotated:
    expectSuccess("directSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall1() }
    expectSuccess("directReceiverAnnotatedSetter.directSetterValueAnnotatedSetter.v2 with context directContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall2() }
    expectSuccess("directSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall3() }
    expectSuccess("directClassReceiverAnnotatedSetter.directSetterValueAnnotatedSetter.v2 with context directClassContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall4() }

    expectSuccess("inlineSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall5() }
    expectSuccess("inlineReceiverAnnotatedSetter.inlineSetterValueAnnotatedSetter.v2 with context inlineContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall6() }
    expectSuccess("inlineSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall7() }
    expectSuccess("inlineClassReceiverAnnotatedSetter.inlineSetterValueAnnotatedSetter.v2 with context inlineClassContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall8() }

    expectSuccess("lambdaSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall9() }
    expectSuccess("lambdaReceiverAnnotatedSetter.lambdaSetterValueAnnotatedSetter.v2 with context lambdaContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall10() }
    expectSuccess("lambdaSetterValueAnnotatedSetter.v2") { ReadWriteSetterAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiverAnnotatedSetter.lambdaSetterValueAnnotatedSetter.v2 with context lambdaClassContextAnnotatedSetter") { ReadWriteSetterAnnotated.directCall12() }
}
