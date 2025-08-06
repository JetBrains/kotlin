fun directCall1() = inlineFunction()
fun directCall2() = inlineFunctionWithParam()
fun directCall3() = inlineFunctionWithParam("custom")
fun directCall4() = with("c") { "receiver".inlineExtensionFunction() }
fun directCall5() = C().inlineClassFunction()
fun directCall6() = C().inlineClassFunctionWithParam()
fun directCall7() = C().inlineClassFunctionWithParam("custom")
fun directCall8() = C().run { with("c") { "receiver".inlineClassExtensionFunction() } }

inline fun inlineCall1() = inlineFunction()
inline fun inlineCall2() = inlineFunctionWithParam()
inline fun inlineCall3() = inlineFunctionWithParam("custom")
inline fun inlineCall4() = with("c") { "receiver".inlineExtensionFunction() }
inline fun inlineCall5() = C().inlineClassFunction()
inline fun inlineCall6() = C().inlineClassFunctionWithParam()
inline fun inlineCall7() = C().inlineClassFunctionWithParam("custom")
inline fun inlineCall8() = C().run { with("c") { "receiver".inlineClassExtensionFunction() } }

inline fun useLambda(f: () -> String) = f()
inline fun lambdaCall1() = useLambda { inlineFunction() }
inline fun lambdaCall2() = useLambda { inlineFunctionWithParam() }
inline fun lambdaCall3() = useLambda { inlineFunctionWithParam("custom") }
inline fun lambdaCall4() = useLambda { with("c") { "receiver".inlineExtensionFunction() } }
inline fun lambdaCall5() = useLambda { C().inlineClassFunction() }
inline fun lambdaCall6() = useLambda { C().inlineClassFunctionWithParam() }
inline fun lambdaCall7() = useLambda { C().inlineClassFunctionWithParam("custom") }
inline fun lambdaCall8() = useLambda { C().run { with("c") { "receiver".inlineClassExtensionFunction() } } }

inline fun defaultParamFunction1(param: String = inlineFunction()) = param
inline fun defaultParamFunction2(param: String = inlineFunctionWithParam()) = param
inline fun defaultParamFunction3(param: String = C().inlineClassFunction()) = param
inline fun defaultParamFunction4(param: String = C().inlineClassFunctionWithParam()) = param