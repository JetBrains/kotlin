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

object ReadOnlyPropertyAnnotated {
    fun directCall1() = excludedReadOnlyInlineProperty
    fun directCall2() = with("directContext") { "directReceiver".excludedReadOnlyInlineExtensionProperty }
    fun directCall3() = C().excludedReadOnlyInlineClassProperty
    fun directCall4() = C().run { with("directClassContext") { "directClassReceiver".excludedReadOnlyInlineClassExtensionProperty } }
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

    inline fun inlineCall1() = excludedReadOnlyInlineProperty
    inline fun inlineCall2() = with("inlineContext") { "inlineReceiver".excludedReadOnlyInlineExtensionProperty }
    inline fun inlineCall3() = C().excludedReadOnlyInlineClassProperty
    inline fun inlineCall4() = C().run { with("inlineClassContext") { "inlineClassReceiver".excludedReadOnlyInlineClassExtensionProperty } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { excludedReadOnlyInlineProperty }
    inline fun lambdaCall2() = useLambda { with("lambdaContext") { "lambdaReceiver".excludedReadOnlyInlineExtensionProperty } }
    inline fun lambdaCall3() = useLambda { C().excludedReadOnlyInlineClassProperty }
    inline fun lambdaCall4() =
        useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".excludedReadOnlyInlineClassExtensionProperty } } }

    inline fun defaultParamFunction1(param: String = excludedReadOnlyInlineProperty) = param
    inline fun defaultParamFunction2(param: String = C().excludedReadOnlyInlineClassProperty) = param
}

object ReadOnlyGetterAnnotated {
    fun directCall1() = excludedReadOnlyInlinePropertyGetter
    fun directCall2() = with("directContext") { "directReceiver".excludedReadOnlyInlineExtensionPropertyGetter }
    fun directCall3() = C().excludedReadOnlyInlineClassPropertyGetter
    fun directCall4() = C().run { with("directClassContext") { "directClassReceiver".excludedReadOnlyInlineClassExtensionPropertyGetter } }
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

    inline fun inlineCall1() = excludedReadOnlyInlinePropertyGetter
    inline fun inlineCall2() = with("inlineContext") { "inlineReceiver".excludedReadOnlyInlineExtensionPropertyGetter }
    inline fun inlineCall3() = C().excludedReadOnlyInlineClassPropertyGetter
    inline fun inlineCall4() = C().run { with("inlineClassContext") { "inlineClassReceiver".excludedReadOnlyInlineClassExtensionPropertyGetter } }

    inline fun useLambda(f: () -> String) = f()
    inline fun lambdaCall1() = useLambda { excludedReadOnlyInlinePropertyGetter }
    inline fun lambdaCall2() = useLambda { with("lambdaContext") { "lambdaReceiver".excludedReadOnlyInlineExtensionPropertyGetter } }
    inline fun lambdaCall3() = useLambda { C().excludedReadOnlyInlineClassPropertyGetter }
    inline fun lambdaCall4() =
        useLambda { C().run { with("lambdaClassContext") { "lambdaClassReceiver".excludedReadOnlyInlineClassExtensionPropertyGetter } } }

    inline fun defaultParamFunction1(param: String = excludedReadOnlyInlinePropertyGetter) = param
    inline fun defaultParamFunction2(param: String = C().excludedReadOnlyInlineClassPropertyGetter) = param
}

object ReadWritePropertyAnnotated {
    fun directCall1(): String {
        excludedReadWriteInlineProperty = "directSetterValue"
        return excludedReadWriteInlineProperty
    }

    fun directCall2(): String  {
        with("directContext") {
            val s = "directReceiver"
            s.excludedReadWriteInlineExtensionProperty = "directSetterValue"
            return s.excludedReadWriteInlineExtensionProperty
        }
    }

    fun directCall3(): String  {
        val c = C()
        c.excludedReadWriteInlineClassProperty = "directSetterValue"
        return c.excludedReadWriteInlineClassProperty
    }

    fun directCall4(): String  {
        C().run {
            with("directClassContext") {
                val s = "directClassReceiver"
                s.excludedReadWriteInlineClassExtensionProperty = "directSetterValue"
                return s.excludedReadWriteInlineClassExtensionProperty
            }
        }
    }

    fun directCall5(): String = inlineCall1()
    fun directCall6(): String = inlineCall2()
    fun directCall7(): String = inlineCall3()
    fun directCall8(): String = inlineCall4()
    fun directCall9(): String = lambdaCall1()
    fun directCall10(): String = lambdaCall2()
    fun directCall11(): String = lambdaCall3()
    fun directCall12(): String = lambdaCall4()

    inline fun inlineCall1(): String {
        excludedReadWriteInlineProperty = "inlineSetterValue"
        return excludedReadWriteInlineProperty
    }

    inline fun inlineCall2(): String  {
        with("inlineContext") {
            val s = "inlineReceiver"
            s.excludedReadWriteInlineExtensionProperty = "inlineSetterValue"
            return s.excludedReadWriteInlineExtensionProperty
        }
    }

    inline fun inlineCall3(): String  {
        val c = C()
        c.excludedReadWriteInlineClassProperty = "inlineSetterValue"
        return c.excludedReadWriteInlineClassProperty
    }

    inline fun inlineCall4(): String  {
        C().run {
            with("inlineClassContext") {
                val s = "inlineClassReceiver"
                s.excludedReadWriteInlineClassExtensionProperty = "inlineSetterValue"
                return s.excludedReadWriteInlineClassExtensionProperty
            }
        }
    }

    inline fun useLambda(f: () -> String) = f()

    inline fun lambdaCall1(): String = useLambda {
        excludedReadWriteInlineProperty = "lambdaSetterValue"
        excludedReadWriteInlineProperty
    }

    inline fun lambdaCall2(): String = useLambda {
        with("lambdaContext") {
            val s = "lambdaReceiver"
            s.excludedReadWriteInlineExtensionProperty = "lambdaSetterValue"
            s.excludedReadWriteInlineExtensionProperty
        }
    }

    inline fun lambdaCall3(): String = useLambda {
        val c = C()
        c.excludedReadWriteInlineClassProperty = "lambdaSetterValue"
        c.excludedReadWriteInlineClassProperty
    }

    inline fun lambdaCall4(): String = useLambda {
        C().run {
            with("lambdaClassContext") {
                val s = "lambdaClassReceiver"
                s.excludedReadWriteInlineClassExtensionProperty = "lambdaSetterValue"
                s.excludedReadWriteInlineClassExtensionProperty
            }
        }
    }
}

object ReadWriteSetterAnnotated {
    fun directCall1(): String {
        excludedReadWriteInlinePropertySetter = "directSetterValueAnnotatedSetter"
        return excludedReadWriteInlinePropertySetter
    }

    fun directCall2(): String  {
        with("directContextAnnotatedSetter") {
            val s = "directReceiverAnnotatedSetter"
            s.excludedReadWriteInlineExtensionPropertySetter = "directSetterValueAnnotatedSetter"
            return s.excludedReadWriteInlineExtensionPropertySetter
        }
    }

    fun directCall3(): String  {
        val c = C()
        c.excludedReadWriteInlineClassPropertySetter = "directSetterValueAnnotatedSetter"
        return c.excludedReadWriteInlineClassPropertySetter
    }

    fun directCall4(): String  {
        C().run {
            with("directClassContextAnnotatedSetter") {
                val s = "directClassReceiverAnnotatedSetter"
                s.excludedReadWriteInlineClassExtensionPropertySetter = "directSetterValueAnnotatedSetter"
                return s.excludedReadWriteInlineClassExtensionPropertySetter
            }
        }
    }

    fun directCall5(): String = inlineCall1()
    fun directCall6(): String = inlineCall2()
    fun directCall7(): String = inlineCall3()
    fun directCall8(): String = inlineCall4()
    fun directCall9(): String = lambdaCall1()
    fun directCall10(): String = lambdaCall2()
    fun directCall11(): String = lambdaCall3()
    fun directCall12(): String = lambdaCall4()

    inline fun inlineCall1(): String {
        excludedReadWriteInlinePropertySetter = "inlineSetterValueAnnotatedSetter"
        return excludedReadWriteInlinePropertySetter
    }

    inline fun inlineCall2(): String  {
        with("inlineContextAnnotatedSetter") {
            val s = "inlineReceiverAnnotatedSetter"
            s.excludedReadWriteInlineExtensionPropertySetter = "inlineSetterValueAnnotatedSetter"
            return s.excludedReadWriteInlineExtensionPropertySetter
        }
    }

    inline fun inlineCall3(): String  {
        val c = C()
        c.excludedReadWriteInlineClassPropertySetter = "inlineSetterValueAnnotatedSetter"
        return c.excludedReadWriteInlineClassPropertySetter
    }

    inline fun inlineCall4(): String  {
        C().run {
            with("inlineClassContextAnnotatedSetter") {
                val s = "inlineClassReceiverAnnotatedSetter"
                s.excludedReadWriteInlineClassExtensionPropertySetter = "inlineSetterValueAnnotatedSetter"
                return s.excludedReadWriteInlineClassExtensionPropertySetter
            }
        }
    }

    inline fun useLambda(f: () -> String) = f()

    inline fun lambdaCall1(): String = useLambda {
        excludedReadWriteInlinePropertySetter = "lambdaSetterValueAnnotatedSetter"
        excludedReadWriteInlinePropertySetter
    }

    inline fun lambdaCall2(): String = useLambda {
        with("lambdaContextAnnotatedSetter") {
            val s = "lambdaReceiverAnnotatedSetter"
            s.excludedReadWriteInlineExtensionPropertySetter = "lambdaSetterValueAnnotatedSetter"
            s.excludedReadWriteInlineExtensionPropertySetter
        }
    }

    inline fun lambdaCall3(): String = useLambda {
        val c = C()
        c.excludedReadWriteInlineClassPropertySetter = "lambdaSetterValueAnnotatedSetter"
        c.excludedReadWriteInlineClassPropertySetter
    }

    inline fun lambdaCall4(): String = useLambda {
        C().run {
            with("lambdaClassContextAnnotatedSetter") {
                val s = "lambdaClassReceiverAnnotatedSetter"
                s.excludedReadWriteInlineClassExtensionPropertySetter = "lambdaSetterValueAnnotatedSetter"
                s.excludedReadWriteInlineClassExtensionPropertySetter
            }
        }
    }
}
