// !DIAGNOSTICS: -UNUSED_PARAMETER

fun interface F1 {
    val functionDelegate: Function<*>? get() = null
    fun invoke()
}

fun interface F2 {
    fun getFunctionDelegate(): Function<*>? = null
    fun invoke()
}

fun interface F3 {
    val getFunctionDelegate: Function<*>? get() = null
    fun invoke()
}

fun interface F4 {
    fun functionDelegate(): Function<*>? = null
    fun invoke()
}

fun interface F5 {
    val functionDelegate: Any? get() = null
    fun invoke()
}

fun interface F6 {
    val String.functionDelegate: Function<*>? get() = null
    fun getFunctionDelegate(x: Any?): Function<*>? = null
    fun invoke()
}
