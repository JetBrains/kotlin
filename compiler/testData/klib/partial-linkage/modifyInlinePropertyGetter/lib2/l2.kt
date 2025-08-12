fun directCall1() = inlineProperty
fun directCall2() = with("directContext") { "directReceiver".inlineExtensionProperty }
fun directCall3() = C().inlineClassProperty
fun directCall4() = C().run { with("directClassContext") { "directClassReceiver".inlineClassExtensionProperty } }
fun directCall5() = inlineCall1()
fun directCall6() = inlineCall2()
fun directCall7() = inlineCall3()
fun directCall8() = inlineCall4()
fun directCall9() = lambdaCall1()
fun directCall10() = lambdaCall2()
fun directCall11() = lambdaCall3()
fun directCall12() = lambdaCall4()
fun directCall13() = defaultParamFunction1()
fun directCall14() = defaultParamFunction2()

inline fun inlineCall1() = inlineProperty
inline fun inlineCall2() = with("inlineContext") { "inlineReceiver".inlineExtensionProperty }
inline fun inlineCall3() = C().inlineClassProperty
inline fun inlineCall4() = C().run { with("inlineClassContext") { "inlineClassReceiver".inlineClassExtensionProperty } }

inline fun useLambda(f: () -> String) = f()
inline fun lambdaCall1() = useLambda { inlineProperty }
inline fun lambdaCall2() = useLambda { with("lambdaContext") { "lambdaReceiver".inlineExtensionProperty } }
inline fun lambdaCall3() = useLambda { C().inlineClassProperty }
inline fun lambdaCall4() = useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".inlineClassExtensionProperty } } }

inline fun defaultParamFunction1(param: String = inlineProperty) = param
inline fun defaultParamFunction2(param: String = C().inlineClassProperty) = param