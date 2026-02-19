fun directCall1() = inlineFunction()
fun directCall2() = inlineFunctionWithParam()
fun directCall3() = inlineFunctionWithParam("directCustomParam")
fun directCall4() = with("directContext") { "directReceiver".inlineExtensionFunction() }
fun directCall5() = C().inlineClassFunction()
fun directCall6() = C().inlineClassFunctionWithParam()
fun directCall7() = C().inlineClassFunctionWithParam("directClassCustomParam")
fun directCall8() = C().run { with("directClassContext") { "directClassReceiver".inlineClassExtensionFunction() } }
fun directCall9() = inlineCall1()
fun directCall10() = inlineCall2()
fun directCall11() = inlineCall3()
fun directCall12() = inlineCall4()
fun directCall13() = inlineCall5()
fun directCall14() = inlineCall6()
fun directCall15() = inlineCall7()
fun directCall16() = inlineCall8()
fun directCall17() = lambdaCall1()
fun directCall18() = lambdaCall2()
fun directCall19() = lambdaCall3()
fun directCall20() = lambdaCall4()
fun directCall21() = lambdaCall5()
fun directCall22() = lambdaCall6()
fun directCall23() = lambdaCall7()
fun directCall24() = lambdaCall8()
fun directCall25() = defaultParamFunction1()
fun directCall26() = defaultParamFunction2()
fun directCall27() = defaultParamFunction3()
fun directCall28() = defaultParamFunction4()

inline fun inlineCall1() = inlineFunction()
inline fun inlineCall2() = inlineFunctionWithParam()
inline fun inlineCall3() = inlineFunctionWithParam("inlineCustomParam")
inline fun inlineCall4() = with("inlineContext") { "inlineReceiver".inlineExtensionFunction() }
inline fun inlineCall5() = C().inlineClassFunction()
inline fun inlineCall6() = C().inlineClassFunctionWithParam()
inline fun inlineCall7() = C().inlineClassFunctionWithParam("inlineClassCustomParam")
inline fun inlineCall8() = C().run { with("inlineClassContext") { "inlineClassReceiver".inlineClassExtensionFunction() } }

inline fun useLambda(f: () -> String) = f()
inline fun lambdaCall1() = useLambda { inlineFunction() }
inline fun lambdaCall2() = useLambda { inlineFunctionWithParam() }
inline fun lambdaCall3() = useLambda { inlineFunctionWithParam("lambdaCustomParam") }
inline fun lambdaCall4() = useLambda { with("lambdaContext") { "lambdaReceiver".inlineExtensionFunction() } }
inline fun lambdaCall5() = useLambda { C().inlineClassFunction() }
inline fun lambdaCall6() = useLambda { C().inlineClassFunctionWithParam() }
inline fun lambdaCall7() = useLambda { C().inlineClassFunctionWithParam("lambdaClassCustomParam") }
inline fun lambdaCall8() = useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".inlineClassExtensionFunction() } } }

inline fun defaultParamFunction1(param: String = inlineFunction()) = param
inline fun defaultParamFunction2(param: String = inlineFunctionWithParam()) = param
inline fun defaultParamFunction3(param: String = C().inlineClassFunction()) = param
inline fun defaultParamFunction4(param: String = C().inlineClassFunctionWithParam()) = param