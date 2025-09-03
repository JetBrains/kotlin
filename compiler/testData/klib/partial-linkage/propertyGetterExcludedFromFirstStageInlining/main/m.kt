import abitestutils.abiTest

fun box() = abiTest {
    // Property annotated
    expectSuccess("excludedInlineProperty.v2") { PropertyAnnotated.directCall1() }
    expectSuccess("directReceiver.excludedInlineExtensionProperty.v2 with context directContext") { PropertyAnnotated.directCall2() }
    expectSuccess("excludedInlineClassProperty.v2") { PropertyAnnotated.directCall3() }
    expectSuccess("directClassReceiver.excludedInlineClassExtensionProperty.v2 with context directClassContext") { PropertyAnnotated.directCall4() }

    expectSuccess("excludedInlineProperty.v2") { PropertyAnnotated.directCall5() }
    expectSuccess("inlineReceiver.excludedInlineExtensionProperty.v2 with context inlineContext") { PropertyAnnotated.directCall6() }
    expectSuccess("excludedInlineClassProperty.v2") { PropertyAnnotated.directCall7() }
    expectSuccess("inlineClassReceiver.excludedInlineClassExtensionProperty.v2 with context inlineClassContext") { PropertyAnnotated.directCall8() }

    expectSuccess("excludedInlineProperty.v2") { PropertyAnnotated.directCall9() }
    expectSuccess("lambdaReceiver.excludedInlineExtensionProperty.v2 with context lambdaContext") { PropertyAnnotated.directCall10() }
    expectSuccess("excludedInlineClassProperty.v2") { PropertyAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiver.excludedInlineClassExtensionProperty.v2 with context lambdaClassContext") { PropertyAnnotated.directCall12() }

    expectSuccess("excludedInlineProperty.v2") { PropertyAnnotated.directCall13() }
    expectSuccess("excludedInlineClassProperty.v2") { PropertyAnnotated.directCall14() }
    
    // Getter annotated
    expectSuccess("excludedInlinePropertyGetter.v2") { GetterAnnotated.directCall1() }
    expectSuccess("directReceiver.excludedInlineExtensionPropertyGetter.v2 with context directContext") { GetterAnnotated.directCall2() }
    expectSuccess("excludedInlineClassPropertyGetter.v2") { GetterAnnotated.directCall3() }
    expectSuccess("directClassReceiver.excludedInlineClassExtensionPropertyGetter.v2 with context directClassContext") { GetterAnnotated.directCall4() }

    expectSuccess("excludedInlinePropertyGetter.v2") { GetterAnnotated.directCall5() }
    expectSuccess("inlineReceiver.excludedInlineExtensionPropertyGetter.v2 with context inlineContext") { GetterAnnotated.directCall6() }
    expectSuccess("excludedInlineClassPropertyGetter.v2") { GetterAnnotated.directCall7() }
    expectSuccess("inlineClassReceiver.excludedInlineClassExtensionPropertyGetter.v2 with context inlineClassContext") { GetterAnnotated.directCall8() }

    expectSuccess("excludedInlinePropertyGetter.v2") { GetterAnnotated.directCall9() }
    expectSuccess("lambdaReceiver.excludedInlineExtensionPropertyGetter.v2 with context lambdaContext") { GetterAnnotated.directCall10() }
    expectSuccess("excludedInlineClassPropertyGetter.v2") { GetterAnnotated.directCall11() }
    expectSuccess("lambdaClassReceiver.excludedInlineClassExtensionPropertyGetter.v2 with context lambdaClassContext") { GetterAnnotated.directCall12() }

    expectSuccess("excludedInlinePropertyGetter.v2") { GetterAnnotated.directCall13() }
    expectSuccess("excludedInlineClassPropertyGetter.v2") { GetterAnnotated.directCall14() }
}