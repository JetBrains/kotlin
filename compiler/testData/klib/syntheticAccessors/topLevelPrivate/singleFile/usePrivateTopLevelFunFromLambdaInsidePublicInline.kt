// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

private fun privateMethod() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline val publicInlineVal: () -> String
    get() = { privateMethod() }

fun box(): String {
    return publicInlineVal.invoke()
}
