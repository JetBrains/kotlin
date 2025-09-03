object PropertyAnnotated {
    fun directCall1(): String {
        excludedInlineProperty = "directSetterValue"
        return excludedInlineProperty
    }

    fun directCall2(): String  {
        with("directContext") {
            val s = "directReceiver"
            s.excludedInlineExtensionProperty = "directSetterValue"
            return s.excludedInlineExtensionProperty
        }
    }

    fun directCall3(): String  {
        val c = C()
        c.excludedInlineClassProperty = "directSetterValue"
        return c.excludedInlineClassProperty
    }

    fun directCall4(): String  {
        C().run {
            with("directClassContext") {
                val s = "directClassReceiver"
                s.excludedInlineClassExtensionProperty = "directSetterValue"
                return s.excludedInlineClassExtensionProperty
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
        excludedInlineProperty = "inlineSetterValue"
        return excludedInlineProperty
    }

    inline fun inlineCall2(): String  {
        with("inlineContext") {
            val s = "inlineReceiver"
            s.excludedInlineExtensionProperty = "inlineSetterValue"
            return s.excludedInlineExtensionProperty
        }
    }

    inline fun inlineCall3(): String  {
        val c = C()
        c.excludedInlineClassProperty = "inlineSetterValue"
        return c.excludedInlineClassProperty
    }

    inline fun inlineCall4(): String  {
        C().run {
            with("inlineClassContext") {
                val s = "inlineClassReceiver"
                s.excludedInlineClassExtensionProperty = "inlineSetterValue"
                return s.excludedInlineClassExtensionProperty
            }
        }
    }

    inline fun useLambda(f: () -> String) = f()

    inline fun lambdaCall1(): String = useLambda {
        excludedInlineProperty = "lambdaSetterValue"
        excludedInlineProperty
    }

    inline fun lambdaCall2(): String = useLambda {
        with("lambdaContext") {
            val s = "lambdaReceiver"
            s.excludedInlineExtensionProperty = "lambdaSetterValue"
            s.excludedInlineExtensionProperty
        }
    }

    inline fun lambdaCall3(): String = useLambda {
        val c = C()
        c.excludedInlineClassProperty = "lambdaSetterValue"
        c.excludedInlineClassProperty
    }

    inline fun lambdaCall4(): String = useLambda {
        C().run {
            with("lambdaClassContext") {
                val s = "lambdaClassReceiver"
                s.excludedInlineClassExtensionProperty = "lambdaSetterValue"
                s.excludedInlineClassExtensionProperty
            }
        }
    }
}

object SetterAnnotated {
    fun directCall1(): String {
        excludedInlinePropertySetter = "directSetterValueAnnotatedSetter"
        return excludedInlinePropertySetter
    }

    fun directCall2(): String  {
        with("directContextAnnotatedSetter") {
            val s = "directReceiverAnnotatedSetter"
            s.excludedInlineExtensionPropertySetter = "directSetterValueAnnotatedSetter"
            return s.excludedInlineExtensionPropertySetter
        }
    }

    fun directCall3(): String  {
        val c = C()
        c.excludedInlineClassPropertySetter = "directSetterValueAnnotatedSetter"
        return c.excludedInlineClassPropertySetter
    }

    fun directCall4(): String  {
        C().run {
            with("directClassContextAnnotatedSetter") {
                val s = "directClassReceiverAnnotatedSetter"
                s.excludedInlineClassExtensionPropertySetter = "directSetterValueAnnotatedSetter"
                return s.excludedInlineClassExtensionPropertySetter
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
        excludedInlinePropertySetter = "inlineSetterValueAnnotatedSetter"
        return excludedInlinePropertySetter
    }

    inline fun inlineCall2(): String  {
        with("inlineContextAnnotatedSetter") {
            val s = "inlineReceiverAnnotatedSetter"
            s.excludedInlineExtensionPropertySetter = "inlineSetterValueAnnotatedSetter"
            return s.excludedInlineExtensionPropertySetter
        }
    }

    inline fun inlineCall3(): String  {
        val c = C()
        c.excludedInlineClassPropertySetter = "inlineSetterValueAnnotatedSetter"
        return c.excludedInlineClassPropertySetter
    }

    inline fun inlineCall4(): String  {
        C().run {
            with("inlineClassContextAnnotatedSetter") {
                val s = "inlineClassReceiverAnnotatedSetter"
                s.excludedInlineClassExtensionPropertySetter = "inlineSetterValueAnnotatedSetter"
                return s.excludedInlineClassExtensionPropertySetter
            }
        }
    }

    inline fun useLambda(f: () -> String) = f()

    inline fun lambdaCall1(): String = useLambda {
        excludedInlinePropertySetter = "lambdaSetterValueAnnotatedSetter"
        excludedInlinePropertySetter
    }

    inline fun lambdaCall2(): String = useLambda {
        with("lambdaContextAnnotatedSetter") {
            val s = "lambdaReceiverAnnotatedSetter"
            s.excludedInlineExtensionPropertySetter = "lambdaSetterValueAnnotatedSetter"
            s.excludedInlineExtensionPropertySetter
        }
    }

    inline fun lambdaCall3(): String = useLambda {
        val c = C()
        c.excludedInlineClassPropertySetter = "lambdaSetterValueAnnotatedSetter"
        c.excludedInlineClassPropertySetter
    }

    inline fun lambdaCall4(): String = useLambda {
        C().run {
            with("lambdaClassContextAnnotatedSetter") {
                val s = "lambdaClassReceiverAnnotatedSetter"
                s.excludedInlineClassExtensionPropertySetter = "lambdaSetterValueAnnotatedSetter"
                s.excludedInlineClassExtensionPropertySetter
            }
        }
    }
}