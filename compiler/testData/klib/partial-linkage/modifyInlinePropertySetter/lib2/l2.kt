fun directCall1(): String {
    inlineProperty = "direct"
    return inlineProperty
}

fun directCall2(): String  {
    with("c") {
        val s = "receiver"
        s.inlineExtensionProperty = "direct"
        return s.inlineExtensionProperty
    }
}

fun directCall3(): String  {
    val c = C()
    c.inlineClassProperty = "direct"
    return c.inlineClassProperty
}

fun directCall4(): String  {
    C().run {
        with("c") {
            val s = "receiver"
            s.inlineClassExtensionProperty = "direct"
            return s.inlineClassExtensionProperty
        }
    }
}

fun inlineCall1(): String {
    inlineProperty = "inline"
    return inlineProperty
}

fun inlineCall2(): String  {
    with("c") {
        val s = "receiver"
        s.inlineExtensionProperty = "inline"
        return s.inlineExtensionProperty
    }
}

fun inlineCall3(): String  {
    val c = C()
    c.inlineClassProperty = "inline"
    return c.inlineClassProperty
}

fun inlineCall4(): String  {
    C().run {
        with("c") {
            val s = "receiver"
            s.inlineClassExtensionProperty = "inline"
            return s.inlineClassExtensionProperty
        }
    }
}

inline fun useLambda(f: () -> String) = f()

fun lambdaCall1(): String = useLambda {
    inlineProperty = "lambda"
    inlineProperty
}

fun lambdaCall2(): String = useLambda {
    with("c") {
        val s = "receiver"
        s.inlineExtensionProperty = "lambda"
        s.inlineExtensionProperty
    }
}

fun lambdaCall3(): String = useLambda {
    val c = C()
    c.inlineClassProperty = "lambda"
    c.inlineClassProperty
}

fun lambdaCall4(): String = useLambda {
    C().run {
        with("c") {
            val s = "receiver"
            s.inlineClassExtensionProperty = "lambda"
            s.inlineClassExtensionProperty
        }
    }
}