import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Property accessor 'topLevelProperty.<get-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<get-topLevelProperty>'")) { getterDirectCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>'")) { getterDirectCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<get-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<get-classProperty>'")) { getterDirectCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<get-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<get-classPropertyWithReceiver>'")) { getterDirectCall4() }

    expectFailure(linkage("Property accessor 'topLevelProperty.<get-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<get-topLevelProperty>'")) { getterInlineCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>'")) { getterInlineCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<get-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<get-classProperty>'")) { getterInlineCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<get-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<get-classPropertyWithReceiver>'")) { getterInlineCall4() }

    expectFailure(linkage("Property accessor 'topLevelProperty.<get-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<get-topLevelProperty>'")) { getterLambdaCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<get-topLevelPropertyWithReceiver>'")) { getterLambdaCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<get-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<get-classProperty>'")) { getterLambdaCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<get-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<get-classPropertyWithReceiver>'")) { getterLambdaCall4() }


    expectFailure(linkage("Property accessor 'topLevelProperty.<set-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<set-topLevelProperty>'")) { setterDirectCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>'")) { setterDirectCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<set-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<set-classProperty>'")) { setterDirectCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<set-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<set-classPropertyWithReceiver>'")) { setterDirectCall4() }

    expectFailure(linkage("Property accessor 'topLevelProperty.<set-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<set-topLevelProperty>'")) { setterInlineCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>'")) { setterInlineCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<set-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<set-classProperty>'")) { setterInlineCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<set-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<set-classPropertyWithReceiver>'")) { setterInlineCall4() }

    expectFailure(linkage("Property accessor 'topLevelProperty.<set-topLevelProperty>' can not be called: No property accessor found for symbol '/topLevelProperty.<set-topLevelProperty>'")) { setterLambdaCall1() }
    expectFailure(linkage("Property accessor 'topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>' can not be called: No property accessor found for symbol '/topLevelPropertyWithReceiver.<set-topLevelPropertyWithReceiver>'")) { setterLambdaCall2() }
    expectFailure(linkage("Property accessor 'classProperty.<set-classProperty>' can not be called: No property accessor found for symbol '/C.classProperty.<set-classProperty>'")) { setterLambdaCall3() }
    expectFailure(linkage("Property accessor 'classPropertyWithReceiver.<set-classPropertyWithReceiver>' can not be called: No property accessor found for symbol '/C.classPropertyWithReceiver.<set-classPropertyWithReceiver>'")) { setterLambdaCall4() }
}