fun directCall1() = topLevelInlineFunction()

fun directCall2() = topLevelInlineFunctionWithParam("directParamValue")

fun directCall3() = with("directContextValue") { "directReceiverValue".topLevelInlineFunctionWithReceiver() }

fun directCall4() = C().classlInlineFunction()

fun directCall5() = C().classlInlineFunctionWithParam("directClassParamValue")

fun directCall6() = C().run { with("directClassContextValue") { "directClassReceiverValue".classlInlineFunctionWithReceiver() } }

fun directCall7() = inlineCall1()

fun directCall8() = inlineCall2()

fun directCall9() = inlineCall3()

fun directCall10() = inlineCall4()

fun directCall11() = inlineCall5()

fun directCall12() = inlineCall6()

fun directCall13() = lambdaCall1()

fun directCall14() = lambdaCall2()

fun directCall15() = lambdaCall3()

fun directCall16() = lambdaCall4()

fun directCall17() = lambdaCall5()

fun directCall18() = lambdaCall6()

inline fun inlineCall1() = topLevelInlineFunction()

inline fun inlineCall2() = topLevelInlineFunctionWithParam("inlineParamValue")

inline fun inlineCall3() = with("inlineContextValue") { "inlineReceiverValue".topLevelInlineFunctionWithReceiver() }

inline fun inlineCall4() = C().classlInlineFunction()

inline fun inlineCall5() = C().classlInlineFunctionWithParam("inlineClassParamValue")

inline fun inlineCall6() = C().run { with("inlineClassContextValue") { "inlineClassReceiverValue".classlInlineFunctionWithReceiver() } }

inline fun useLambda(f: () -> String) = f()

inline fun lambdaCall1() = useLambda { topLevelInlineFunction() }

inline fun lambdaCall2() = useLambda { topLevelInlineFunctionWithParam("lambdaParamValue") }

inline fun lambdaCall3() = useLambda { with("lambdaContextValue") { "lambdaReceiverValue".topLevelInlineFunctionWithReceiver() } }

inline fun lambdaCall4() = useLambda { C().classlInlineFunction() }

inline fun lambdaCall5() = useLambda { C().classlInlineFunctionWithParam("lambdaClassParamValue") }

inline fun lambdaCall6() = useLambda { C().run { with("lambdaClassContextValue") { "lambdaClassReceiverValue".classlInlineFunctionWithReceiver() } } }