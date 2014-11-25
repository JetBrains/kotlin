// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    [nativeInvoke]
    fun Int.ext() = 1

    [nativeInvoke]
    fun Int.invoke(a: String, b: Int) = "OK"
}