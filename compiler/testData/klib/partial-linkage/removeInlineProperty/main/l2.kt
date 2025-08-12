fun getterDirectCall1() = topLevelProperty

fun getterDirectCall2() = with("directContextValue") { "directReceiverValue".topLevelPropertyWithReceiver }

fun getterDirectCall3() = C().classProperty

fun getterDirectCall4() = C().run { with("directClassContextValue") { "directClassReceiverValue".classPropertyWithReceiver } }

fun getterDirectCall5() = getterInlineCall1()

fun getterDirectCall6() = getterInlineCall2()

fun getterDirectCall7() = getterInlineCall3()

fun getterDirectCall8() = getterInlineCall4()

fun getterDirectCall9() = getterLambdaCall1()

fun getterDirectCall10() = getterLambdaCall2()

fun getterDirectCall11() = getterLambdaCall3()

fun getterDirectCall12() = getterLambdaCall4()

fun setterDirectCall1(): String {
    topLevelProperty = "directSetterValue"
    return _topLevelProperty
}

fun setterDirectCall2(): String {
    with("directContextValue") { "directReceiverValue".topLevelPropertyWithReceiver = "directSetterValue" }
    return _topLevelPropertyWithReceiver
}

fun setterDirectCall3(): String {
    val c = C()
    c.classProperty = "directClassSetterValue"
    return c._classProperty
}

fun setterDirectCall4(): String {
    val c = C()
    c.run { with("directClassContextValue") { "directClassReceiverValue".classPropertyWithReceiver = "directClassSetterValue" } }
    return c._classPropertyWithReceiver
}

fun setterDirectCall5(): String = setterInlineCall1()

fun setterDirectCall6(): String = setterInlineCall2()

fun setterDirectCall7(): String = setterInlineCall3()

fun setterDirectCall8(): String = setterInlineCall4()

fun setterDirectCall9(): String = setterLambdaCall1()

fun setterDirectCall10(): String = setterLambdaCall2()

fun setterDirectCall11(): String = setterLambdaCall3()

fun setterDirectCall12(): String = setterLambdaCall4()

inline fun getterInlineCall1() = topLevelProperty

inline fun getterInlineCall2() = with("inlineContextValue") { "inlineReceiverValue".topLevelPropertyWithReceiver }

inline fun getterInlineCall3() = C().classProperty

inline fun getterInlineCall4() = C().run { with("inlineClassContextValue") { "inlineClassReceiverValue".classPropertyWithReceiver } }

inline fun callGetterFromLambda(f: () -> String) = f()

inline fun getterLambdaCall1() = callGetterFromLambda { topLevelProperty }

inline fun getterLambdaCall2() = callGetterFromLambda { with("lambdaContextValue") { "lambdaReceiverValue".topLevelPropertyWithReceiver } }

inline fun getterLambdaCall3() = callGetterFromLambda { C().classProperty }

inline fun getterLambdaCall4() =
    callGetterFromLambda { C().run { with("lambdaClassContextValue") { "lambdaClassReceiverValue".classPropertyWithReceiver } } }

inline fun setterInlineCall1(): String {
    topLevelProperty = "inlineSetterValue"
    return _topLevelProperty
}

inline fun setterInlineCall2(): String {
    with("inlineContextValue") { "inlineReceiverValue".topLevelPropertyWithReceiver = "inlineSetterValue" }
    return _topLevelPropertyWithReceiver
}

inline fun setterInlineCall3(): String {
    val c = C()
    c.classProperty = "inlineClassSetterValue"
    return c._classProperty
}

inline fun setterInlineCall4(): String {
    val c = C()
    c.run { with("inlineClassContextValue") { "inlineClassReceiverValue".classPropertyWithReceiver = "inlineClassSetterValue" } }
    return c._classPropertyWithReceiver
}

inline fun callSetterFromLambda(f: () -> String) = f()

inline fun setterLambdaCall1() = callSetterFromLambda {
    topLevelProperty = "lambdaSetterValue"
    _topLevelProperty
}

inline fun setterLambdaCall2() = callSetterFromLambda {
    with("lambdaContextValue") { "lambdaReceiverValue".topLevelPropertyWithReceiver = "lambdaSetterValue" }
    _topLevelPropertyWithReceiver
}

inline fun setterLambdaCall3() = callSetterFromLambda {
    val c = C()
    c.classProperty = "lambdaClassSetterValue"
    c._classProperty
}

inline fun setterLambdaCall4() = callSetterFromLambda {
    val c = C()
    c.run { with("lambdaClassContextValue") { "lambdaClassReceiverValue".classPropertyWithReceiver = "lambdaClassSetterValue" } }
    c._classPropertyWithReceiver
}