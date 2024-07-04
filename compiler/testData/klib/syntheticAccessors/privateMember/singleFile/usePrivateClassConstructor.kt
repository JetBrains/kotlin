private class Private{
    fun foo() = "OK"
}

internal inline fun internalInlineFun(): String {
    @Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE")
    return Private().foo()
}

fun box(): String {
    return internalInlineFun()
}
