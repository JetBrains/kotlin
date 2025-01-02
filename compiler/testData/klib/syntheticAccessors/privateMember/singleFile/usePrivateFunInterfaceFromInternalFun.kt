//IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

private fun interface I {
    fun foo(): Int
}

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION")
inline fun publicInlineFun(): Int = (I { 1 }).foo()

@Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE", "IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION")
internal inline fun internalInlineFun(): Int = (I { 1 }).foo()

fun box(): String {
    var result = 0
    result += publicInlineFun()
    result += internalInlineFun()
    return if (result == 2) "OK" else result.toString()
}
