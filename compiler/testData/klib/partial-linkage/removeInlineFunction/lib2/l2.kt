fun directCall1() = topLevelInlineFunction()

fun directCall2() = topLevelInlineFunctionWithParam("")

fun directCall3() = with("") { "".topLevelInlineFunctionWithReceiver() }

fun directCall4() = C().classlInlineFunction()

fun directCall5() = C().classlInlineFunctionWithParam("")

fun directCall6() = C().run { with("") { "".classlInlineFunctionWithReceiver() } }

inline fun inlineCall1() = topLevelInlineFunction()

inline fun inlineCall2() = topLevelInlineFunctionWithParam("")

inline fun inlineCall3() = with("") { "".topLevelInlineFunctionWithReceiver() }

inline fun inlineCall4() = C().classlInlineFunction()

inline fun inlineCall5() = C().classlInlineFunctionWithParam("")

inline fun inlineCall6() = C().run { with("") { "".classlInlineFunctionWithReceiver() } }

inline fun useLambda(f: () -> String) = f()

inline fun lambdaCall1() = useLambda { topLevelInlineFunction() }

inline fun lambdaCall2() = useLambda { topLevelInlineFunctionWithParam("") }

inline fun lambdaCall3() = useLambda { with("") { "".topLevelInlineFunctionWithReceiver() } }

inline fun lambdaCall4() = useLambda { C().classlInlineFunction() }

inline fun lambdaCall5() = useLambda { C().classlInlineFunctionWithParam("") }

inline fun lambdaCall6() = useLambda { C().run { with("") { "".classlInlineFunctionWithReceiver() } } }