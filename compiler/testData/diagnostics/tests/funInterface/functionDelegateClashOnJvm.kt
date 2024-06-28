// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface F1 {
    val <!FUNCTION_DELEGATE_MEMBER_NAME_CLASH!>functionDelegate<!>: Function<*>? get() = null
    fun invoke()
}

fun interface F2 {
    fun <!FUNCTION_DELEGATE_MEMBER_NAME_CLASH!>getFunctionDelegate<!>(): Function<*>? = null
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
    val <!FUNCTION_DELEGATE_MEMBER_NAME_CLASH!>functionDelegate<!>: Any? get() = null
    fun invoke()
}

fun interface F6 {
    val String.functionDelegate: Function<*>? get() = null
    fun getFunctionDelegate(x: Any?): Function<*>? = null
    fun invoke()
}
