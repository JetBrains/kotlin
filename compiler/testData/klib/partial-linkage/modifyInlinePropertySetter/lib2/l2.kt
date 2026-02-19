fun directCall1(): String {
    inlineProperty = "directSetterValue"
    return inlineProperty
}

fun directCall2(): String  {
    with("directContext") {
        val s = "directReceiver"
        s.inlineExtensionProperty = "directSetterValue"
        return s.inlineExtensionProperty
    }
}

fun directCall3(): String  {
    val c = C()
    c.inlineClassProperty = "directSetterValue"
    return c.inlineClassProperty
}

fun directCall4(): String  {
    C().run {
        with("directClassContext") {
            val s = "directClassReceiver"
            s.inlineClassExtensionProperty = "directSetterValue"
            return s.inlineClassExtensionProperty
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

fun inlineCall1(): String {
    inlineProperty = "inlineSetterValue"
    return inlineProperty
}

fun inlineCall2(): String  {
    with("inlineContext") {
        val s = "inlineReceiver"
        s.inlineExtensionProperty = "inlineSetterValue"
        return s.inlineExtensionProperty
    }
}

fun inlineCall3(): String  {
    val c = C()
    c.inlineClassProperty = "inlineSetterValue"
    return c.inlineClassProperty
}

fun inlineCall4(): String  {
    C().run {
        with("inlineClassContext") {
            val s = "inlineClassReceiver"
            s.inlineClassExtensionProperty = "inlineSetterValue"
            return s.inlineClassExtensionProperty
        }
    }
}

inline fun useLambda(f: () -> String) = f()

fun lambdaCall1(): String = useLambda {
    inlineProperty = "lambdaSetterValue"
    inlineProperty
}

fun lambdaCall2(): String = useLambda {
    with("lambdaContext") {
        val s = "lambdaReceiver"
        s.inlineExtensionProperty = "lambdaSetterValue"
        s.inlineExtensionProperty
    }
}

fun lambdaCall3(): String = useLambda {
    val c = C()
    c.inlineClassProperty = "lambdaSetterValue"
    c.inlineClassProperty
}

fun lambdaCall4(): String = useLambda {
    C().run {
        with("lambdaClassContext") {
            val s = "lambdaClassReceiver"
            s.inlineClassExtensionProperty = "lambdaSetterValue"
            s.inlineClassExtensionProperty
        }
    }
}