private val privateVal: String = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = ::privateVal

fun box(): String {
    return publicInlineFunction().invoke()
}
