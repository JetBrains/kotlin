// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

sealed class Sealed(val x: S)

class Test(x: S) : Sealed(x)

fun box() = Test(S("OK")).x.string