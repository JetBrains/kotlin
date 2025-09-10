// WITH_STDLIB
// DISABLE_IR_VISIBILITY_CHECKS: ANY

// NB 'b' is evaluated before 's'
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline fun invokeNoInline(noinline b: (String) -> String, s: String) =
    b(s)

fun box(): String = invokeNoInline({ it + "K" }, "O")
