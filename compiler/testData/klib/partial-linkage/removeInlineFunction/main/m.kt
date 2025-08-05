import abitestutils.abiTest

fun box() = abiTest {
    expectFailure(linkage("Function 'topLevelInlineFunction' can not be called: No function found for symbol '/topLevelInlineFunction'")) { directCall1() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithParam' can not be called: No function found for symbol '/topLevelInlineFunctionWithParam'")) { directCall2() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithReceiver' can not be called: No function found for symbol '/topLevelInlineFunctionWithReceiver'")) { directCall3() }
    expectFailure(linkage("Function 'classlInlineFunction' can not be called: No function found for symbol '/C.classlInlineFunction'")) { directCall4() }
    expectFailure(linkage("Function 'classlInlineFunctionWithParam' can not be called: No function found for symbol '/C.classlInlineFunctionWithParam'")) { directCall5() }
    expectFailure(linkage("Function 'classlInlineFunctionWithReceiver' can not be called: No function found for symbol '/C.classlInlineFunctionWithReceiver'")) { directCall6() }

    expectFailure(linkage("Function 'topLevelInlineFunction' can not be called: No function found for symbol '/topLevelInlineFunction'")) { inlineCall1() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithParam' can not be called: No function found for symbol '/topLevelInlineFunctionWithParam'")) { inlineCall2() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithReceiver' can not be called: No function found for symbol '/topLevelInlineFunctionWithReceiver'")) { inlineCall3() }
    expectFailure(linkage("Function 'classlInlineFunction' can not be called: No function found for symbol '/C.classlInlineFunction'")) { inlineCall4() }
    expectFailure(linkage("Function 'classlInlineFunctionWithParam' can not be called: No function found for symbol '/C.classlInlineFunctionWithParam'")) { inlineCall5() }
    expectFailure(linkage("Function 'classlInlineFunctionWithReceiver' can not be called: No function found for symbol '/C.classlInlineFunctionWithReceiver'")) { inlineCall6() }

    expectFailure(linkage("Function 'topLevelInlineFunction' can not be called: No function found for symbol '/topLevelInlineFunction'")) { lambdaCall1() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithParam' can not be called: No function found for symbol '/topLevelInlineFunctionWithParam'")) { lambdaCall2() }
    expectFailure(linkage("Function 'topLevelInlineFunctionWithReceiver' can not be called: No function found for symbol '/topLevelInlineFunctionWithReceiver'")) { lambdaCall3() }
    expectFailure(linkage("Function 'classlInlineFunction' can not be called: No function found for symbol '/C.classlInlineFunction'")) { lambdaCall4() }
    expectFailure(linkage("Function 'classlInlineFunctionWithParam' can not be called: No function found for symbol '/C.classlInlineFunctionWithParam'")) { lambdaCall5() }
    expectFailure(linkage("Function 'classlInlineFunctionWithReceiver' can not be called: No function found for symbol '/C.classlInlineFunctionWithReceiver'")) { lambdaCall6() }
}