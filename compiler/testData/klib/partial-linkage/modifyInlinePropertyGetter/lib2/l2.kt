fun directCall1() = inlineProperty
fun directCall2() = with("c") { "receiver".inlineExtensionProperty }
fun directCall3() = C().inlineClassProperty
fun directCall4() = C().run { with("c") { "receiver".inlineClassExtensionProperty } }

inline fun inlineCall1() = inlineProperty
inline fun inlineCall2() = with("c") { "receiver".inlineExtensionProperty }
inline fun inlineCall3() = C().inlineClassProperty
inline fun inlineCall4() = C().run { with("c") { "receiver".inlineClassExtensionProperty } }

inline fun useLambda(f: () -> String) = f()
inline fun lambdaCall1() = useLambda { inlineProperty }
inline fun lambdaCall2() = useLambda { with("c") { "receiver".inlineExtensionProperty } }
inline fun lambdaCall3() = useLambda { C().inlineClassProperty }
inline fun lambdaCall4() = useLambda { C().run { with("c") { "receiver".inlineClassExtensionProperty } } }

inline fun defaultParamFunction1(param: String = inlineProperty) = param
inline fun defaultParamFunction2(param: String = C().inlineClassProperty) = param