object InlineFunctionFull {
    fun directCall1() = inlineFunctionFull()
    fun directCall2() = inlineFunctionWithParamFull()
    fun directCall3() = inlineFunctionWithParamFull("directCustomParamFull")
    fun directCall4() = with("directContextFull") { "directReceiverFull".inlineExtensionFunctionFull() }
    fun directCall5() = C().inlineClassFunctionFull()
    fun directCall6() = C().inlineClassFunctionWithParamFull()
    fun directCall7() = C().inlineClassFunctionWithParamFull("directClassCustomParamFull")
    fun directCall8() = C().run { with("directClassContextFull") { "directClassReceiverFull".inlineClassExtensionFunctionFull() } }
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

    inline fun inlineCall1() = inlineFunctionFull()
    inline fun inlineCall2() = inlineFunctionWithParamFull()
    inline fun inlineCall3() = inlineFunctionWithParamFull("inlineCustomParamFull")
    inline fun inlineCall4() = with("inlineContextFull") { "inlineReceiverFull".inlineExtensionFunctionFull() }
    inline fun inlineCall5() = C().inlineClassFunctionFull()
    inline fun inlineCall6() = C().inlineClassFunctionWithParamFull()
    inline fun inlineCall7() = C().inlineClassFunctionWithParamFull("inlineClassCustomParamFull")
    inline fun inlineCall8() = C().run { with("inlineClassContextFull") { "inlineClassReceiverFull".inlineClassExtensionFunctionFull() } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlineFunctionFull() }
    inline fun lambdaCall2() = useLambda { inlineFunctionWithParamFull() }
    inline fun lambdaCall3() = useLambda { inlineFunctionWithParamFull("lambdaCustomParamFull") }
    inline fun lambdaCall4() = useLambda { with("lambdaContextFull") { "lambdaReceiverFull".inlineExtensionFunctionFull() } }
    inline fun lambdaCall5() = useLambda { C().inlineClassFunctionFull() }
    inline fun lambdaCall6() = useLambda { C().inlineClassFunctionWithParamFull() }
    inline fun lambdaCall7() = useLambda { C().inlineClassFunctionWithParamFull("lambdaClassCustomParamFull") }
    inline fun lambdaCall8() = useLambda { C().run { with("lambdaClassContextFull") { "lambdaClassReceiverFull".inlineClassExtensionFunctionFull() } } }

    inline fun defaultParamFunction1(param: String = inlineFunctionFull()) = param
    inline fun defaultParamFunction2(param: String = inlineFunctionWithParamFull()) = param
    inline fun defaultParamFunction3(param: String = C().inlineClassFunctionFull()) = param
    inline fun defaultParamFunction4(param: String = C().inlineClassFunctionWithParamFull()) = param
}

object InlineFunctionDisabled {
    fun directCall1() = inlineFunctionDisabled()
    fun directCall2() = inlineFunctionWithParamDisabled()
    fun directCall3() = inlineFunctionWithParamDisabled("directCustomParamDisabled")
    fun directCall4() = with("directContextDisabled") { "directReceiverDisabled".inlineExtensionFunctionDisabled() }
    fun directCall5() = D().inlineClassFunctionDisabled()
    fun directCall6() = D().inlineClassFunctionWithParamDisabled()
    fun directCall7() = D().inlineClassFunctionWithParamDisabled("directClassCustomParamDisabled")
    fun directCall8() = D().run { with("directClassContextDisabled") { "directClassReceiverDisabled".inlineClassExtensionFunctionDisabled() } }
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

    inline fun inlineCall1() = inlineFunctionDisabled()
    inline fun inlineCall2() = inlineFunctionWithParamDisabled()
    inline fun inlineCall3() = inlineFunctionWithParamDisabled("inlineCustomParamDisabled")
    inline fun inlineCall4() = with("inlineContextDisabled") { "inlineReceiverDisabled".inlineExtensionFunctionDisabled() }
    inline fun inlineCall5() = D().inlineClassFunctionDisabled()
    inline fun inlineCall6() = D().inlineClassFunctionWithParamDisabled()
    inline fun inlineCall7() = D().inlineClassFunctionWithParamDisabled("inlineClassCustomParamDisabled")
    inline fun inlineCall8() = D().run { with("inlineClassContextDisabled") { "inlineClassReceiverDisabled".inlineClassExtensionFunctionDisabled() } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlineFunctionDisabled() }
    inline fun lambdaCall2() = useLambda { inlineFunctionWithParamDisabled() }
    inline fun lambdaCall3() = useLambda { inlineFunctionWithParamDisabled("lambdaCustomParamDisabled") }
    inline fun lambdaCall4() = useLambda { with("lambdaContextDisabled") { "lambdaReceiverDisabled".inlineExtensionFunctionDisabled() } }
    inline fun lambdaCall5() = useLambda { D().inlineClassFunctionDisabled() }
    inline fun lambdaCall6() = useLambda { D().inlineClassFunctionWithParamDisabled() }
    inline fun lambdaCall7() = useLambda { D().inlineClassFunctionWithParamDisabled("lambdaClassCustomParamDisabled") }
    inline fun lambdaCall8() = useLambda { D().run { with("lambdaClassContextDisabled") { "lambdaClassReceiverDisabled".inlineClassExtensionFunctionDisabled() } } }

    inline fun defaultParamFunction1(param: String = inlineFunctionDisabled()) = param
    inline fun defaultParamFunction2(param: String = inlineFunctionWithParamDisabled()) = param
    inline fun defaultParamFunction3(param: String = D().inlineClassFunctionDisabled()) = param
    inline fun defaultParamFunction4(param: String = D().inlineClassFunctionWithParamDisabled()) = param
}

object InlineFunctionIntraModule {
    fun directCall1() = inlineFunctionIntraModule()
    fun directCall2() = inlineFunctionWithParamIntraModule()
    fun directCall3() = inlineFunctionWithParamIntraModule("directCustomParamIntraModule")
    fun directCall4() = with("directContextIntraModule") { "directReceiverIntraModule".inlineExtensionFunctionIntraModule() }
    fun directCall5() = E().inlineClassFunctionIntraModule()
    fun directCall6() = E().inlineClassFunctionWithParamIntraModule()
    fun directCall7() = E().inlineClassFunctionWithParamIntraModule("directClassCustomParamIntraModule")
    fun directCall8() = E().run { with("directClassContextIntraModule") { "directClassReceiverIntraModule".inlineClassExtensionFunctionIntraModule() } }
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

    inline fun inlineCall1() = inlineFunctionIntraModule()
    inline fun inlineCall2() = inlineFunctionWithParamIntraModule()
    inline fun inlineCall3() = inlineFunctionWithParamIntraModule("inlineCustomParamIntraModule")
    inline fun inlineCall4() = with("inlineContextIntraModule") { "inlineReceiverIntraModule".inlineExtensionFunctionIntraModule() }
    inline fun inlineCall5() = E().inlineClassFunctionIntraModule()
    inline fun inlineCall6() = E().inlineClassFunctionWithParamIntraModule()
    inline fun inlineCall7() = E().inlineClassFunctionWithParamIntraModule("inlineClassCustomParamIntraModule")
    inline fun inlineCall8() = E().run { with("inlineClassContextIntraModule") { "inlineClassReceiverIntraModule".inlineClassExtensionFunctionIntraModule() } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlineFunctionIntraModule() }
    inline fun lambdaCall2() = useLambda { inlineFunctionWithParamIntraModule() }
    inline fun lambdaCall3() = useLambda { inlineFunctionWithParamIntraModule("lambdaCustomParamIntraModule") }
    inline fun lambdaCall4() = useLambda { with("lambdaContextIntraModule") { "lambdaReceiverIntraModule".inlineExtensionFunctionIntraModule() } }
    inline fun lambdaCall5() = useLambda { E().inlineClassFunctionIntraModule() }
    inline fun lambdaCall6() = useLambda { E().inlineClassFunctionWithParamIntraModule() }
    inline fun lambdaCall7() = useLambda { E().inlineClassFunctionWithParamIntraModule("lambdaClassCustomParamIntraModule") }
    inline fun lambdaCall8() = useLambda { E().run { with("lambdaClassContextIntraModule") { "lambdaClassReceiverIntraModule".inlineClassExtensionFunctionIntraModule() } } }

    inline fun defaultParamFunction1(param: String = inlineFunctionIntraModule()) = param
    inline fun defaultParamFunction2(param: String = inlineFunctionWithParamIntraModule()) = param
    inline fun defaultParamFunction3(param: String = E().inlineClassFunctionIntraModule()) = param
    inline fun defaultParamFunction4(param: String = E().inlineClassFunctionWithParamIntraModule()) = param
}

object InlinePropertyFull {
    fun directCall1() = inlinePropertyFull
    fun directCall2() = with("directContextFull") { "directReceiverFull".inlineExtensionPropertyFull }
    fun directCall3() = C().inlineClassPropertyFull
    fun directCall4() = C().run { with("directClassContextFull") { "directClassReceiverFull".inlineClassExtensionPropertyFull } }
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

    inline fun inlineCall1() = inlinePropertyFull
    inline fun inlineCall2() = with("inlineContextFull") { "inlineReceiverFull".inlineExtensionPropertyFull }
    inline fun inlineCall3() = C().inlineClassPropertyFull
    inline fun inlineCall4() = C().run { with("inlineClassContextFull") { "inlineClassReceiverFull".inlineClassExtensionPropertyFull } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlinePropertyFull }
    inline fun lambdaCall2() = useLambda { with("lambdaContextFull") { "lambdaReceiverFull".inlineExtensionPropertyFull } }
    inline fun lambdaCall3() = useLambda { C().inlineClassPropertyFull }
    inline fun lambdaCall4() = useLambda { C().run { with("lambdaClassContextFull") { "lambdaClassReceiverFull".inlineClassExtensionPropertyFull } } }

    inline fun defaultParamFunction1(param: String = inlinePropertyFull) = param
    inline fun defaultParamFunction2(param: String = C().inlineClassPropertyFull) = param
}

object InlinePropertyDisabled {
    fun directCall1() = inlinePropertyDisabled
    fun directCall2() = with("directContextDisabled") { "directReceiverDisabled".inlineExtensionPropertyDisabled }
    fun directCall3() = D().inlineClassPropertyDisabled
    fun directCall4() = D().run { with("directClassContextDisabled") { "directClassReceiverDisabled".inlineClassExtensionPropertyDisabled } }
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

    inline fun inlineCall1() = inlinePropertyDisabled
    inline fun inlineCall2() = with("inlineContextDisabled") { "inlineReceiverDisabled".inlineExtensionPropertyDisabled }
    inline fun inlineCall3() = D().inlineClassPropertyDisabled
    inline fun inlineCall4() = D().run { with("inlineClassContextDisabled") { "inlineClassReceiverDisabled".inlineClassExtensionPropertyDisabled } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlinePropertyDisabled }
    inline fun lambdaCall2() = useLambda { with("lambdaContextDisabled") { "lambdaReceiverDisabled".inlineExtensionPropertyDisabled } }
    inline fun lambdaCall3() = useLambda { D().inlineClassPropertyDisabled }
    inline fun lambdaCall4() = useLambda { D().run { with("lambdaClassContextDisabled") { "lambdaClassReceiverDisabled".inlineClassExtensionPropertyDisabled } } }

    inline fun defaultParamFunction1(param: String = inlinePropertyDisabled) = param
    inline fun defaultParamFunction2(param: String = D().inlineClassPropertyDisabled) = param
}

object InlinePropertyIntraModule {
    fun directCall1() = inlinePropertyIntraModule
    fun directCall2() = with("directContextIntraModule") { "directReceiverIntraModule".inlineExtensionPropertyIntraModule }
    fun directCall3() = E().inlineClassPropertyIntraModule
    fun directCall4() = E().run { with("directClassContextIntraModule") { "directClassReceiverIntraModule".inlineClassExtensionPropertyIntraModule } }
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

    inline fun inlineCall1() = inlinePropertyIntraModule
    inline fun inlineCall2() = with("inlineContextIntraModule") { "inlineReceiverIntraModule".inlineExtensionPropertyIntraModule }
    inline fun inlineCall3() = E().inlineClassPropertyIntraModule
    inline fun inlineCall4() = E().run { with("inlineClassContextIntraModule") { "inlineClassReceiverIntraModule".inlineClassExtensionPropertyIntraModule } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { inlinePropertyIntraModule }
    inline fun lambdaCall2() = useLambda { with("lambdaContextIntraModule") { "lambdaReceiverIntraModule".inlineExtensionPropertyIntraModule } }
    inline fun lambdaCall3() = useLambda { E().inlineClassPropertyIntraModule }
    inline fun lambdaCall4() = useLambda { E().run { with("lambdaClassContextIntraModule") { "lambdaClassReceiverIntraModule".inlineClassExtensionPropertyIntraModule } } }

    inline fun defaultParamFunction1(param: String = inlinePropertyIntraModule) = param
    inline fun defaultParamFunction2(param: String = E().inlineClassPropertyIntraModule) = param
}