// WITH_STDLIB

inline fun new(init: (Z) -> Unit): Z = Z(42)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val value: Int)

fun box(): String =
    if (new(fun(z: Z) {}).value == 42) "OK" else "Fail"
