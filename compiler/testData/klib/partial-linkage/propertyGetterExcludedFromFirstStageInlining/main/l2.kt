object PropertyAnnotated {
    fun directCall1() = excludedInlineProperty
    fun directCall2() = with("directContext") { "directReceiver".excludedInlineExtensionProperty }
    fun directCall3() = C().excludedInlineClassProperty
    fun directCall4() = C().run { with("directClassContext") { "directClassReceiver".excludedInlineClassExtensionProperty } }
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

    inline fun inlineCall1() = excludedInlineProperty
    inline fun inlineCall2() = with("inlineContext") { "inlineReceiver".excludedInlineExtensionProperty }
    inline fun inlineCall3() = C().excludedInlineClassProperty
    inline fun inlineCall4() = C().run { with("inlineClassContext") { "inlineClassReceiver".excludedInlineClassExtensionProperty } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { excludedInlineProperty }
    inline fun lambdaCall2() = useLambda { with("lambdaContext") { "lambdaReceiver".excludedInlineExtensionProperty } }
    inline fun lambdaCall3() = useLambda { C().excludedInlineClassProperty }
    inline fun lambdaCall4() =
        useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".excludedInlineClassExtensionProperty } } }

    inline fun defaultParamFunction1(param: String = excludedInlineProperty) = param
    inline fun defaultParamFunction2(param: String = C().excludedInlineClassProperty) = param
}

object GetterAnnotated {
    fun directCall1() = excludedInlinePropertyGetter
    fun directCall2() = with("directContext") { "directReceiver".excludedInlineExtensionPropertyGetter }
    fun directCall3() = C().excludedInlineClassPropertyGetter
    fun directCall4() = C().run { with("directClassContext") { "directClassReceiver".excludedInlineClassExtensionPropertyGetter } }
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

    inline fun inlineCall1() = excludedInlinePropertyGetter
    inline fun inlineCall2() = with("inlineContext") { "inlineReceiver".excludedInlineExtensionPropertyGetter }
    inline fun inlineCall3() = C().excludedInlineClassPropertyGetter
    inline fun inlineCall4() = C().run { with("inlineClassContext") { "inlineClassReceiver".excludedInlineClassExtensionPropertyGetter } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { excludedInlinePropertyGetter }
    inline fun lambdaCall2() = useLambda { with("lambdaContext") { "lambdaReceiver".excludedInlineExtensionPropertyGetter } }
    inline fun lambdaCall3() = useLambda { C().excludedInlineClassPropertyGetter }
    inline fun lambdaCall4() =
        useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".excludedInlineClassExtensionPropertyGetter } } }

    inline fun defaultParamFunction1(param: String = excludedInlinePropertyGetter) = param
    inline fun defaultParamFunction2(param: String = C().excludedInlineClassPropertyGetter) = param
}