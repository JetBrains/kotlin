private class Private{
    fun foo() = "OK"
}

@Suppress("IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR")
internal inline fun internalInlineFun(): String {
    @Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE")
    return Private().foo()
}

fun box(): String {
    return internalInlineFun()
}
