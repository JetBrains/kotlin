// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class X(val x: Any)

fun useX(x: X): String = x.x as String

fun <T> call(fn: () -> T) = fn()

fun box() = useX(call { X("OK") })