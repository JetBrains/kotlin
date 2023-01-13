// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

@nativeInvoke
fun Int.ext() = 1

@nativeInvoke
fun Int.invoke(a: String, b: Int) = "OK"
